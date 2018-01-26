package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.trade.params.RippleWithdrawFundsParams;

import arbitrail.libra.model.Account;
import arbitrail.libra.model.Accounts;
import arbitrail.libra.model.Currencies;
import arbitrail.libra.model.MyCurrency;
import arbitrail.libra.model.Wallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.utils.ExchCcy;
import arbitrail.libra.utils.Parser;
import arbitrail.libra.utils.Transformer;
import arbitrail.libra.utils.Utils;

public class BalancerServiceImpl implements BalancerService {
	
	private final static Logger LOG = Logger.getLogger(BalancerServiceImpl.class);
	
	private Double balanceCheckThreshold;
	private Boolean simulate;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<String, String> pendingTransIdToToExchMap; //TODO need to persist this
	
	public BalancerServiceImpl(Properties properties, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<String, String> pendingTransIdToToExchMap) {
		balanceCheckThreshold = Double.valueOf(properties.getProperty(Utils.Props.balance_check_threshold.name()));
		simulate = Boolean.valueOf(properties.getProperty(Utils.Props.simulate.name()));
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.pendingTransIdToToExchMap = pendingTransIdToToExchMap;
	}

	@Override
	public Map<Exchange, AccountInfo> connectToExchanges(List<Exchange> exchangeList) {
		Map<Exchange, AccountInfo> exchangeMap = new HashMap<>();
		
		for (Exchange exchange : exchangeList) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();
			
			AccountInfo accountInfo;
			try {
				accountInfo = exchange.getAccountService().getAccountInfo();
			} catch (NotAvailableFromExchangeException | NotYetImplementedForExchangeException | ExchangeException
					| IOException e) {
				LOG.error("Failed to get account info for the exchange :" + exchangeName, e);
				continue;
			}
			
			exchangeMap.put(exchange, accountInfo);
		}
		
		return exchangeMap;
	}

	@Override
	public List<Currency> listAllHandledCurrencies() {
		List<Currency> currencyList = new ArrayList<>();
		
		try {
			Currencies currencies = Parser.parseCurrencies();
			for (MyCurrency myCurrency : currencies.getCurrency()) {
				currencyList.add(Transformer.fromCurrency(myCurrency));
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return currencyList;
	}
	
	@Override
	public List<Exchange> listAllHandledAccounts() {
		List<Exchange> exchangeList = new ArrayList<>();
		
		try {
			Accounts accounts = Parser.parseAccounts();
			for (Account account : accounts.getAccount()) {
				BaseExchange exchange = Transformer.fromAccount(account);
				exchangeList.add(createExchange(exchange, account.getUsername(), account.getApiKey(), account.getKey()));
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return exchangeList;
	}
	
	@Override
	public Wallets loadAllAccountsBalance(Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList, boolean init) {
		Wallets wallets = null;
		
		try {
			if (Parser.existsWalletsFile()) {
				wallets = Parser.parseWallets();
				if (init) {
					addNewWallets(wallets, exchangeMap, currencyList);
				}
			} else {
				if (!init) {
					LOG.error("The wallets file does not exist or could not be found - please check the file or run Libra in init mode");
					return null;
				}
				Map<String, Map<String, Wallet>> walletMap = initWallets(exchangeMap, currencyList);
				wallets = new Wallets(walletMap);
			}

		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return wallets;
	}

	/*
	 * Adds only new wallets from the list of exchanges and currencies to the existing wallets map
	 */
	private void addNewWallets(Wallets wallets, Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList) {
		Map<String, Map<String, Wallet>> walletMap = wallets.getWalletMap();
		
		for (Entry<Exchange, AccountInfo> entry : exchangeMap.entrySet()) {
			Exchange toExchange = entry.getKey();
			String exchangeName = toExchange.getExchangeSpecification().getExchangeName();
			
			Map<String, Wallet> currencyMap = walletMap.get(exchangeName);
			if (currencyMap == null) {
				LOG.debug("New exchanged to be added to wallets file : " + exchangeName);
				currencyMap = new HashMap<>();
				walletMap.put(exchangeName, currencyMap);
			}
			
			for (Currency currency : currencyList) {
				Wallet wallet = currencyMap.get(currency.getCurrencyCode());
				if (wallet == null) {
					Balance balance = entry.getValue().getWallet().getBalance(currency);
					if (BigDecimal.ZERO.equals(balance.getAvailable())) {
						LOG.warn("Currency not available : " + currency.getDisplayName() +  " for Exchange : " + exchangeName);
					} else {
						LOG.debug("New currency wallet for : " + exchangeName + " -> " + currency.getDisplayName());
						wallet = new Wallet(balance.getAvailable(), balance.getAvailable());
						currencyMap.put(currency.getCurrencyCode(), wallet);
					}
				}
			}
		}
	}

	/*
	 * Creates a new wallets map from the list of exchanges and currencies
	 */
	private Map<String, Map<String, Wallet>> initWallets(Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList) {
		Map<String, Map<String, Wallet>> walletMap = new HashMap<>();
		
		for (Entry<Exchange, AccountInfo> entry : exchangeMap.entrySet()) {
			Exchange toExchange = entry.getKey();
			String exchangeName = toExchange.getExchangeSpecification().getExchangeName();
			HashMap<String, Wallet> currencyMap = new HashMap<>();
			walletMap.put(exchangeName, currencyMap);
			
			for (Currency currency : currencyList) {
				Balance balance = entry.getValue().getWallet().getBalance(currency);
				if (BigDecimal.ZERO.equals(balance.getAvailable())) {
					LOG.warn("Currency not available : " + currency.getDisplayName() +  " for Exchange : " + exchangeName);
				} else {
					Wallet wallet = new Wallet(balance.getAvailable(), balance.getAvailable());
					currencyMap.put(currency.getCurrencyCode(), wallet);
				}
			}
		}

		return walletMap;
	}
	
	@Override
	public Exchange findMostFilledBalance(Map<Exchange, AccountInfo> exchangeMap, Currency currency) {
		Exchange maxExchange = null;
		BigDecimal maxBalance = BigDecimal.ZERO;
		
		for (Entry<Exchange, AccountInfo> entry : exchangeMap.entrySet()) {
			Balance balance = entry.getValue().getWallet().getBalance(currency);
			if (maxBalance.compareTo(balance.getAvailable()) < 0) {
				maxExchange = entry.getKey();
				maxBalance = balance.getAvailable();
			}
		}
		
		return maxExchange;
	}
	
	@Override
	public int balanceAccounts(Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList, Wallets wallets) {
		Map<String, Map<String, Wallet>> walletMap = wallets.getWalletMap();
		int nbOperations = 0;
		
		for (Entry<Exchange, AccountInfo> entry : exchangeMap.entrySet()) {
			Exchange toExchange = entry.getKey();
			String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
			Map<String, Wallet> toExchangeWallets = walletMap.get(toExchangeName);
			// no currencies set up for the exchange
			if (toExchangeWallets == null) {
				LOG.warn("No wallet config found for destination account : " + toExchangeName);
				LOG.info("Skipping exchange : " + toExchangeName);
				continue;
			}
			
			for (Currency currency : currencyList) {
				Wallet toWallet = toExchangeWallets.get(currency.getCurrencyCode());
				// this currency is not set up for the exchange
				if (toWallet == null) {
					LOG.warn("No wallet config found for destination account for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}

				// do the rebalancing only if there is no pending withdrawal
				Boolean pendingWithdrawal = pendingWithdrawalsMap.get(new ExchCcy(toExchangeName, currency));
				if (pendingWithdrawal != null && pendingWithdrawal) {
					LOG.info("Pending withdrawal : Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}
				
				// there has been no rebalancing yet and the pending service may not be running
				else if (pendingWithdrawal == null) {
					LOG.info("No balancing has been done yet for " + toExchangeName + " -> " + currency.getDisplayName());
				}
				
				// the threshold represents the minimum amount from which the balance will be triggered
				Balance currentBalance = entry.getValue().getWallet().getBalance(currency);
				BigDecimal checkThresholdBalance = toWallet.maxBalance().multiply(new BigDecimal(balanceCheckThreshold));
				LOG.debug("Exchange : " + toExchangeName + " -> " + currency.getDisplayName() + " / checkThresholdBalance = " + checkThresholdBalance + " / currentBalance = " + currentBalance.getAvailable());

				// trigger the balancer
				if (currentBalance.getAvailable().compareTo(checkThresholdBalance) < 0) {
					LOG.info("Exchange needs to be balanced for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					Exchange fromExchange = findMostFilledBalance(exchangeMap, currency);
					if (fromExchange.equals(toExchange)) {
						LOG.error("Unexpected error : The source and the destination accounts are identical");
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
					Map<String, Wallet> fromExchangeWallets = walletMap.get(fromExchangeName);
					if (fromExchangeWallets == null) {
						LOG.error("Unexpected error : Missing wallet config for source account : " + fromExchangeName);
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					Wallet fromWallet = fromExchangeWallets.get(currency.getCurrencyCode());
					if (fromWallet == null) {
						LOG.error("Unexpected error : Missing wallet config for source account for currency : " + fromExchangeName + " -> " + currency.getDisplayName());
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					try {
						// the rebalancing actually occured
						if (balance(toExchange, fromExchange, currency, fromWallet.getMinResidualBalance(), toWallet.getPaymentIdForXRP())) {
							// TODO must update thoses balances once the withdrawal is complete
							Balance newIncreasedBalance = entry.getValue().getWallet().getBalance(currency);
							LOG.debug("newIncreasedBalance for " + toExchangeName + " -> " + currency.getDisplayName());
							Balance newDecreasedBalance = exchangeMap.get(fromExchange).getWallet().getBalance(currency);
							LOG.debug("newDecreasedBalance for " + fromExchangeName + " -> " + currency.getDisplayName());
							toWallet.setLastBalancedAmount(newIncreasedBalance.getAvailable());
							fromWallet.setLastBalancedAmount(newDecreasedBalance.getAvailable());
							nbOperations++;
						}
						
					} catch (NotAvailableFromExchangeException | NotYetImplementedForExchangeException
							| ExchangeException | IOException | IllegalStateException e) {
						LOG.error("Exception occured when trying to balance accounts", e);
					}
				}
			}
		}
		return nbOperations;
	}

	@Override
	public boolean balance(Exchange toExchange, Exchange fromExchange, Currency currency, BigDecimal minResidualBalance, String paymentIdforXRP) throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
		String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
		String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
		
		Balance toBalance = toExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
		Balance fromBalance = fromExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
		LOG.info("Source exchange [" + fromExchangeName + " -> " + currency.getDisplayName() + "] balance : "
				+ fromBalance.getAvailable() + " / Destination exchange [" + toExchangeName + " -> "
				+ currency.getDisplayName() + "] balance : " + toBalance.getAvailable());
		
		BigDecimal balancedOffset = fromBalance.getAvailable().subtract(toBalance.getAvailable()).divide(BigDecimal.valueOf(2));
		BigDecimal allowedWithdrawableAmount = fromBalance.getAvailable().subtract(minResidualBalance);
		BigDecimal amountToWithdraw = balancedOffset.min(allowedWithdrawableAmount);
		LOG.debug("amountToWithdraw = min (balancedOffset, allowedWithdrawableAmount) = min (" + balancedOffset + ", " + allowedWithdrawableAmount + ")");
		
		// amountToWithdraw cannot be negative
		if (BigDecimal.ZERO.compareTo(amountToWithdraw) >= 0) {
			LOG.error("Withdraw amount can't be negative or 0 - please decrease the minResidualBalance = " + minResidualBalance + " for " + fromExchangeName + " -> " + currency.getDisplayName());
			return false;
		}
		
		LOG.info("amountToWithdraw [" + fromExchangeName + " -> " + toExchangeName + "] : " + amountToWithdraw);
		
 		String depositAddress = toExchange.getAccountService().requestDepositAddress(currency);
		LOG.debug("Deposit address [" + toExchangeName + " -> " + currency.getDisplayName() + "] : " + depositAddress);

		if (!simulate) {
			String transactionId;
			if (Currency.XRP.equals(currency)) {
				transactionId = fromExchange.getAccountService().withdrawFunds(new RippleWithdrawFundsParams(depositAddress, currency, amountToWithdraw, paymentIdforXRP));
			} else {
				transactionId = fromExchange.getAccountService().withdrawFunds(currency, amountToWithdraw, depositAddress);
			}
			LOG.debug("transactionId = " + transactionId);
			
			// set map pending transactions to true
			ExchCcy exchCcy = new ExchCcy(toExchangeName, currency);
			pendingWithdrawalsMap.put(exchCcy, true);

			// add mapping destination exchange to transactionId
			pendingTransIdToToExchMap.put(transactionId, toExchangeName);
			
			return true;
		}
		
		return false;
	}

}

package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.bitfinex.v1.service.BitfinexAccountService;
import org.knowm.xchange.bittrex.service.BittrexAccountService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import arbitrail.libra.exchange.AliasCode;
import arbitrail.libra.model.CurrencyAttribute;
import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.MyWallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.orm.service.WalletService;
import si.mazi.rescu.HttpStatusIOException;

@Component
public class BalancerService implements Runnable {
	
	private final static Logger LOG = Logger.getLogger(BalancerService.class);

	private static final int NUM_ATTEMPTS = 25;
	
	@Autowired
	private TransxIdToTargetExchService transxIdToTargetExchService;
	
	@Autowired
	private PendingTransxService pendingTransxService;

	@Autowired
	private WalletService walletService;

	@Autowired
	private TransactionService transxService;

	@Value( "${withdrawalWaitingDelay}" )
	private Integer withdrawalWaitingDelay;

	@Value( "${balancer_frequency}" )
	private Integer frequency;
	
	@Value( "${simulate}" )
	private Boolean simulate;
	
	@Value( "${balance_check_threshold}" )
	private Double balanceCheckThreshold;
	
	private Wallets wallets;
	private Map<Exchange, String> exchangeMap;
	private List<Currency> currencyList;
	private Map<String, CurrencyAttribute> currencyAttributesMap;
	private ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	private Map<String, Set<String>> withdrawTestMap;
	
	public BalancerService() {
		super();
	}

	public void init(Wallets wallets, ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, List<Currency> currencies,
			Map<String, CurrencyAttribute> currencyAttributesMap, Map<Exchange, String> exchangeMap) {
		this.currencyList = currencies;
		this.exchangeMap = exchangeMap;
		this.wallets = wallets;
		this.currencyAttributesMap = currencyAttributesMap;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.withdrawTestMap = initWithdrawTestMap();
	}
	
	private Map<String, Set<String>> initWithdrawTestMap() {
		Map<String, Set<String>> withdrawTestMap = new HashMap<>();
		Set<Exchange> exchangeSet = exchangeMap.keySet();
		
		for (Entry<String, CurrencyAttribute> attr : currencyAttributesMap.entrySet()) {
			if (attr.getValue().isTest()) {
				withdrawTestMap.put(attr.getKey(), exchangeSet.stream().map(e -> e.getExchangeSpecification().getExchangeName()).collect(Collectors.toSet()));
			}
		}

		return withdrawTestMap;
	}

	private Map<Exchange, Map<Currency, Balance>> getAllBalances(Map<Exchange, String> exchangeMap) throws IOException {
		Map<Exchange, Map<Currency, Balance>> balancesMap = new HashMap<>();
		
		for (Exchange exchange : exchangeMap.keySet()) {
			try {
				Map<Currency, Balance> balancesForExchange = walletService.getAvailableBalances(exchange, exchangeMap.get(exchange));
				balancesMap.put(exchange, balancesForExchange);
			} catch (ExchangeException e) {
				String exchangeName = exchange.getExchangeSpecification().getExchangeName();
				LOG.error("Exception occured when trying to get the balances for exchange : " + exchangeName, e);
				LOG.info("Skipping balances for exchange : " + exchangeName);
			}
		}
		
		return balancesMap;
	}
	
	private Exchange findMostFilledBalance(Set<Exchange> exchangeSet, Map<Exchange, Map<Currency, Balance>> balancesMap, Map<String, Map<String, MyWallet>> walletMap, Currency currency) throws IOException {
		Exchange maxExchange = null;
		BigDecimal maxBalance = BigDecimal.ZERO;
		
		for (Exchange exchange : exchangeSet) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();
			Map<String, MyWallet> walletsForExchange = walletMap.get(exchangeName);
			// no currencies set up for the exchange
			if (walletsForExchange == null) {
				LOG.warn("Configuration error : Could not find any wallet configuration for account : " + exchangeName);
				LOG.info("Skipping exchange : " + exchangeName);
				continue;
			}
			MyWallet myWallet = walletsForExchange.get(currency.getCurrencyCode());
			// this currency is not set up for the exchange
			if (myWallet == null) {
				LOG.warn("Configuration error : No wallet config found for this account for currency : " + exchangeName + "$" + currency.getCurrencyCode());
				LOG.info("Skipping currency for exchange : " + exchangeName + "$" + currency.getCurrencyCode());
				continue;
			}
			Map<Currency, Balance> balancesForExchange = balancesMap.get(exchange);
			if (balancesForExchange == null) {
				LOG.info("findMostFilledBalance : No balances retrieved for exchange : " + exchangeName);
				continue;
			}
			Balance balanceForCurrency;
			try {
				balanceForCurrency = walletService.getBalancesForExchange(exchangeName, currency, balancesForExchange);
			} catch (Exception e) {
				LOG.error(e.getMessage());
				LOG.info("Skipping currency for exchange : " + exchangeName + "$" + currency.getCurrencyCode());
				continue;
			}
			
			BigDecimal balance = balanceForCurrency == null ? BigDecimal.ZERO : balanceForCurrency.getAvailable();
			if (maxBalance.compareTo(balance) < 0) {
				maxExchange = exchange;
				maxBalance = balance;
			}
		}
		
		return maxExchange;
	}
	
	private int balanceAccounts() throws IOException {
		// get all balances for wallet of currencies for each exchange - avoids multiple calls to getBalance per currency for a specific exchange
		Map<Exchange, Map<Currency, Balance>> balanceMap = getAllBalances(exchangeMap);
		Map<String, Map<String, MyWallet>> walletMap = wallets.getWalletMap();
		int nbOperations = 0;
		int setPrecision = 4;
		
		for (Exchange toExchange : exchangeMap.keySet()) {
			String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
			Map<Currency, Balance> balancesForExchange = balanceMap.get(toExchange);
			if (balancesForExchange == null) {
				LOG.info("No balances retrieved - skipping exchange : " + toExchangeName);
				continue;
			}
			
			Map<String, MyWallet> toExchangeWallets = walletMap.get(toExchangeName);
			// no currencies set up for the exchange
			if (toExchangeWallets == null) {
				LOG.warn("Configuration error : No wallet config found for destination account : " + toExchangeName);
				LOG.info("Skipping exchange : " + toExchangeName);
				continue;
			}
			
			for (Currency currency : currencyList) {
				String currencyCode = currency.getCurrencyCode();
				MyWallet toWallet = toExchangeWallets.get(currencyCode);
				// this currency is not set up for the exchange
				if (toWallet == null) {
					LOG.warn("Configuration error : No wallet config found for destination account for currency : " + toExchangeName + "$" + currency.getCurrencyCode());
					LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
					continue;
				}

				// do the rebalancing only if there is no pending withdrawal
				ExchCcy exchCcy = new ExchCcy(toExchangeName, currencyCode);
				if (pendingWithdrawalsMap.keySet().contains(exchCcy)) {
					LOG.info("# Pending withdrawal : Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
					continue;
				}
				
				Balance balanceForCurrency;
				try {
					balanceForCurrency = walletService.getBalancesForExchange(toExchangeName, currency, balancesForExchange);
				} catch (Exception e) {
					LOG.error(e.getMessage());
					LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
					continue;
				}
				
				BigDecimal currentBalance = balanceForCurrency == null ? BigDecimal.ZERO : balanceForCurrency.getAvailable();
				// the threshold represents the minimum amount from which the balance will be triggered
				BigDecimal checkThresholdBalance = walletService.getAvgBalance(currency, balanceMap).multiply(new BigDecimal(balanceCheckThreshold), new MathContext(setPrecision, RoundingMode.FLOOR));
				LOG.debug("# Exchange : " + toExchangeName + "$" + currency.getCurrencyCode() + " : currentBalance = " + currentBalance + " / checkThresholdBalance = " + checkThresholdBalance);

				// trigger the balancer
				if (currentBalance.compareTo(checkThresholdBalance) < 0) {
					LOG.info("## Exchange needs to be balanced for currency : " + toExchangeName + "$" + currency.getCurrencyCode());
					Exchange fromExchange = findMostFilledBalance(exchangeMap.keySet(), balanceMap, walletMap, currency);
					if (fromExchange == null) {
						LOG.error("Balancing error : All the accounts balances are zero");
						LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					if (fromExchange.equals(toExchange)) {
						LOG.error("Balancing error : The source and the destination accounts are identical");
						LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					
					String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
					Map<String, MyWallet> fromExchangeWallets = walletMap.get(fromExchangeName);
					if (fromExchangeWallets == null) {
						LOG.warn("Configuration error : Missing wallet config for source account : " + fromExchangeName);
						LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					MyWallet fromWallet = fromExchangeWallets.get(currencyCode);
					if (fromWallet == null) {
						LOG.warn("Configuration error : Missing wallet config for source account for currency : " + fromExchangeName + "$" + currency.getCurrencyCode());
						LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					
					try {
						if (balance(fromExchange, toExchange, currency, currencyAttributesMap.get(currencyCode), fromWallet, toWallet, balanceMap)) {
							// the rebalancing actually occured
							BigDecimal newDecreasedBalance = walletService.getAvailableBalance(fromExchange, exchangeMap.get(fromExchange), currency);
							LOG.info("new provisional balance for " + fromExchangeName + "$" + currency.getCurrencyCode() + " : " + newDecreasedBalance);
							nbOperations++;
						}
						
					} catch (Exception e) {
						LOG.error("Exception occured when trying to balance accounts", e);
					}
				}
			}
		}
		return nbOperations;
	}
	
	private String withdrawFunds(Exchange exchange, String withdrawAddress, Currency currency, BigDecimal amountToWithdraw, String paymentId, BigDecimal fees) throws IOException, InterruptedException {
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
			HitbtcAccountService hitbtcAccountService = (HitbtcAccountService) exchange.getAccountService();
			hitbtcAccountService.transferToMain(currency, amountToWithdraw);
			int nbTry = 3;
			while (--nbTry > 0) {
				try {
					// the amount to withdraw from hitbtc does not contain the fees (sum of withdrawal and deposit fees)
					BigDecimal amountDeducted = amountToWithdraw.subtract(fees);
					LOG.info("### Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " ["
							+ exchangeName + "$" + currency.getCurrencyCode() + "] amount: " + amountDeducted);
					return hitbtcAccountService.withdrawFundsRaw(currency, amountDeducted, withdrawAddress, paymentId);
				} catch (HttpStatusIOException hse) {
					if (nbTry == 1) {
						// revert the withdraw
						hitbtcAccountService.transferToTrading(currency, amountToWithdraw);
						throw hse;
					}
					Thread.sleep(2000);
				}
			}
			return null;
		}
		else {
			// the withdrawal fee has been added to the amount to withdraw
			LOG.info("### Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " [" + exchangeName + "$" + currency.getCurrencyCode() + "] amount: " + amountToWithdraw);
			
			if (ExchangeType.Binance.name().equals(exchangeName)) {
				BinanceAccountService binanceAccountService = (BinanceAccountService) exchange.getAccountService();
				
				if (paymentId == null) {
					return binanceAccountService.withdraw(currency.getCurrencyCode(), withdrawAddress, amountToWithdraw);
				} else {
					return binanceAccountService.withdraw(currency.getCurrencyCode(), withdrawAddress, paymentId, amountToWithdraw);
				}
			}
			
			else if (ExchangeType.Bittrex.name().equals(exchangeName)) {
				BittrexAccountService bittrexAccountService = (BittrexAccountService) exchange.getAccountService();
				return bittrexAccountService.withdraw(currency.getCurrencyCode(), amountToWithdraw, withdrawAddress, paymentId);
			}
			
			else if (ExchangeType.BitFinex.name().equals(exchangeName)) {
				BitfinexAccountService bitfinexAccountService = (BitfinexAccountService) exchange.getAccountService();
				return bitfinexAccountService.withdrawFunds(currency, amountToWithdraw, withdrawAddress, paymentId);
			}

			else {
				LOG.info("No implementation of withdrawFunds for this exchange : " + exchangeName);
				return null;
			}
		}
	}
	
	private boolean balance(Exchange fromExchange, Exchange toExchange, Currency currency, CurrencyAttribute currencyAttribute, MyWallet fromWallet, MyWallet toWallet, Map<Exchange, Map<Currency, Balance>> balanceMap) throws Exception {
		String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
		String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
		
		Balance	fromBalance = walletService.getBalancesForExchange(fromExchangeName, currency, balanceMap.get(fromExchange));
		Balance	toBalance = walletService.getBalancesForExchange(toExchangeName, currency, balanceMap.get(toExchange));
		
		BigDecimal fromBalanceAvailable = fromBalance == null ? BigDecimal.ZERO : fromBalance.getAvailable();
		BigDecimal toBalanceAvailable = toBalance == null ? BigDecimal.ZERO : toBalance.getAvailable();

		LOG.info("Source exchange [" + fromExchangeName + "$" + currency.getCurrencyCode() + "] balance : "
				+ fromBalanceAvailable + " -> Destination exchange [" + toExchangeName + "$"
				+ currency.getCurrencyCode() + "] balance : " + toBalanceAvailable);
		
		BigDecimal balancedOffset = fromBalanceAvailable.subtract(toBalanceAvailable).divide(BigDecimal.valueOf(2));
		BigDecimal allowedWithdrawableAmount = fromBalanceAvailable.subtract(fromWallet.getMinResidualBalance());
		// we add the withdrawal fee to the amount to withdraw as it will be deducted automatically in order to get the desired withdrawal amount
		BigDecimal amountToWithdraw = transxService.roundAmount(balancedOffset.min(allowedWithdrawableAmount), currency).add(fromWallet.getWithdrawalFee());
		LOG.debug("## amountToWithdraw = min (balancedOffset, allowedWithdrawableAmount) + withdrawalFee = min ("
				+ balancedOffset + ", " + allowedWithdrawableAmount + ") + " + fromWallet.getWithdrawalFee());
		
		// test mode - the withdrawal amount is bounded by the maxTestAmount
		if (currencyAttribute.isTest() && withdrawTestMap.get(currency.getCurrencyCode()).contains(fromExchangeName)) {
			if (currencyAttribute.getMaxTestAmount() == null) {
				LOG.error("MaxTestAmount is null whereas the test mode is set to true - please provide a value for maxTestAmount for the currency : " + currency.getCurrencyCode());
				return false;
			}
			amountToWithdraw = amountToWithdraw.min(currencyAttribute.getMaxTestAmount());
			LOG.debug("--- Test mode enabled for this currency : " + currency.getCurrencyCode() + " - maxTestAmount: " + currencyAttribute.getMaxTestAmount());
		}

		// normal mode - the minWithdrawalAmount is computed
		else if (!currencyAttribute.isTest()) {
			// amountToWithdraw must be higher than the minimum amount specified in the config
			BigDecimal minWithdrawalAmount = walletService.getMinWithdrawalAmount(fromWallet, toWallet, currency, balanceMap);
			if (amountToWithdraw.compareTo(minWithdrawalAmount) < 0) {
				LOG.error("Withdraw amount is lower than minWithdrawAmount = " + amountToWithdraw + " / " + minWithdrawalAmount + " for " + fromExchangeName + "$" + currency.getCurrencyCode());
				return false;
			}
		}
		
		String depositAddress = null;
		// normal mode or test mode not done yet for the currency
		if (!currencyAttribute.isTest() || withdrawTestMap.get(currency.getCurrencyCode()).contains(fromExchangeName)) {
			LOG.info("## amountToWithdraw [" + fromExchangeName + " -> " + toExchangeName + "] : " + amountToWithdraw);

			depositAddress = walletService.getDepositAddress(toExchange, toExchangeName, toWallet, currency);
			if (depositAddress == null) {
				LOG.error("Unexpected error : The deposit address of the destination wallet is null");
				LOG.info("Skipping currency for exchange : " + toExchangeName + "$" + currency.getCurrencyCode());
				return false;
			}
			LOG.debug("Deposit address [" + toExchangeName + "$" + currency.getCurrencyCode() + "] : " + depositAddress);
		}

		if (!simulate) {
			BigDecimal fees = fromWallet.getWithdrawalFee().add(toWallet.getDepositFee());
			
			String internalId = null;
			// test mode not done yet for the currency
			if (currencyAttribute.isTest() && withdrawTestMap.get(currency.getCurrencyCode()).contains(fromExchangeName)) {
				LOG.warn("--- Testing withdrawal for this currency [" + fromExchangeName + "$" + currency.getCurrencyCode() + "] - amountToWithdraw: " + amountToWithdraw);
				internalId = withdrawFunds(fromExchange, depositAddress, currency, amountToWithdraw, toWallet.getTag(), fees);
				withdrawTestMap.get(currency.getCurrencyCode()).remove(fromExchangeName);
			} 
			// normal mode
			else if (!currencyAttribute.isTest()) {
				LOG.info("### Withdrawing funds for this currency [" + fromExchangeName + "$" + currency.getCurrencyCode() + "] - amountToWithdraw: " + amountToWithdraw);
				internalId = withdrawFunds(fromExchange, depositAddress, currency, amountToWithdraw, toWallet.getTag(), fees);
			}
			
			if (internalId != null) {
				LOG.debug("internalId = " + internalId);
				
				// load the history in order to retrieve the matching externalId (transactionId) from the internalId returned by the withdrawFunds method
				Integer transxHashkey = -1;
				int numAttempts = 0;
				do {
					try {
						LOG.debug("Waiting for the transactionId... sleeping for (ms) : " + withdrawalWaitingDelay);
						Thread.sleep(withdrawalWaitingDelay);
						
						List<FundingRecord> fundingRecords = fromExchange.getAccountService().getFundingHistory(transxService.getTradeHistoryParams(fromExchange, currency));	
						Optional<FundingRecord> matchingFundingRecord = transxService.filterByInternalId(fundingRecords, internalId);
						if (matchingFundingRecord.isPresent()) {
							// the amount that will be deposited in the target account will be deducted from the deposit fee
							Currency recordCurrency = matchingFundingRecord.get().getCurrency();
							if (ExchangeType.BitFinex.name().equals(fromExchangeName)) {
								String aliasCode = AliasCode.getGenericCode(recordCurrency.getCurrencyCode());
								if (aliasCode == null) {
									throw new Exception("Could not find the generic alias code for bitfinex currency : " + recordCurrency.getCurrencyCode());
								}
								recordCurrency = new Currency(aliasCode);
							}
							
							BigDecimal depositAmount = transxService.roundAmount(matchingFundingRecord.get().getAmount(), recordCurrency).subtract(toWallet.getDepositFee());
							transxHashkey = transxService.transxHashkey(recordCurrency, depositAmount, depositAddress);
							LOG.debug("## transxHashkey : " + transxHashkey + " = (" + recordCurrency + ", " + depositAmount + ", " + depositAddress + ")");
						}
						numAttempts++;
						if (numAttempts == NUM_ATTEMPTS) {
							throw new Exception("Maximum number of attempts reached: " + numAttempts);
						}
					} catch (HttpStatusIOException hse) {
						LOG.error("Exchange exception : ", hse);
						
					} catch (Exception e) {
						LOG.fatal("Unexpected error : Cannot monitor pending withdrawal for " + fromExchangeName + "$" + currency.getCurrencyCode() + " with transactionId = " + internalId);
						LOG.fatal("Unexpected exception : ", e);
						LOG.fatal("Libra has stopped!");
						System.exit(-1);
					}
				} while (transxHashkey == -1);
				
				// add pending transactions to the map
				ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
				pendingWithdrawalsMap.put(exchCcy, new Object());
				LOG.debug("Saving the pending withdrawal...");
				pendingTransxService.save(exchCcy);
				
				// add mapping destination exchange to transactionId
				ExchStatus exchStatus = new ExchStatus(toExchangeName, false, Calendar.getInstance().getTime());
				transxIdToTargetExchMap.put(transxHashkey, exchStatus);
				LOG.debug("Saving the transaction Id...");
				transxIdToTargetExchService.save(new AbstractMap.SimpleEntry<Integer, ExchStatus>(transxHashkey, exchStatus));
				
				return true;
			} else {
				LOG.warn("WARNING! There is no withdrawal to proceed. All currencies may be in test mode and the tests have been completed.");
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public void run() {
		int nbOperations;
		LOG.info("Balancer service has started!");
		while (true) {
			try {
				LocalDateTime before = LocalDateTime.now();
				nbOperations = balanceAccounts();
				LocalDateTime after = LocalDateTime.now();
				LOG.info("Number of rebalancing operations : " + nbOperations + " performed in (ms) : " + ChronoUnit.MILLIS.between(before, after));
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (Exception e) {
				LOG.fatal("Unexpected exception : ", e);
			}
		}
	}

}

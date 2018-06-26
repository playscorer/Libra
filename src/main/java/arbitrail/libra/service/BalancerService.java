package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.knowm.xchange.hitbtc.v2.service.HitbtcFundingHistoryParams;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.RippleWithdrawFundsParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	
	private int withdrawalWaitingDelay = 30000;
	
	@Autowired
	private TransxIdToTargetExchService transxIdToTargetExchService;
	
	@Autowired
	private PendingTransxService pendingTransxService;

	@Autowired
	private WalletService walletService;

	@Autowired
	private TransactionService transxService;

	@Value( "${balancer_frequency}" )
	private Integer frequency;
	
	@Value( "${simulate}" )
	private Boolean simulate;
	
	@Value( "${balance_check_threshold}" )
	private Double balanceCheckThreshold;
	
	private Wallets wallets;
	private Map<Exchange, String> exchangeMap;
	private List<Currency> currencies;
	private ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	
	public BalancerService() {
		super();
	}

	public void init(Wallets wallets, ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, List<Currency> currencies,
			Map<Exchange, String> exchangeMap) {
		this.currencies = currencies;
		this.exchangeMap = exchangeMap;
		this.wallets = wallets;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
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
				LOG.warn("Configuration error : No wallet config found for this account for currency : " + exchangeName + " -> " + currency.getDisplayName());
				LOG.info("Skipping currency for exchange : " + exchangeName + " -> " + currency.getDisplayName());
				continue;
			}
			Map<Currency, Balance> balancesForExchange = balancesMap.get(exchange);
			if (balancesForExchange == null) {
				LOG.info("No balances retrieved - skipping exchange : " + exchangeName);
				continue;
			}
			Balance balanceForCurrency = balancesForExchange.get(currency);
			BigDecimal balance = balanceForCurrency == null ? BigDecimal.ZERO : balanceForCurrency.getAvailable();
			if (maxBalance.compareTo(balance) < 0) {
				maxExchange = exchange;
				maxBalance = balance;
			}
		}
		
		return maxExchange;
	}
	
	private int balanceAccounts(Map<Exchange, String> exchangeMap, List<Currency> currencyList, Wallets wallets) throws IOException, InterruptedException {
		// get all balances for wallet of currencies for each exchange - avoids multiple calls to getBalance per currency for a specific exchange
		Map<Exchange, Map<Currency, Balance>> balanceMap = getAllBalances(exchangeMap);
		Map<String, Map<String, MyWallet>> walletMap = wallets.getWalletMap();
		int nbOperations = 0;
		
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
					LOG.warn("Configuration error : No wallet config found for destination account for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}

				// do the rebalancing only if there is no pending withdrawal
				ExchCcy exchCcy = new ExchCcy(toExchangeName, currencyCode);
				if (pendingWithdrawalsMap.keySet().contains(exchCcy)) {
					LOG.info("# Pending withdrawal : Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}
				
				// the threshold represents the minimum amount from which the balance will be triggered
				Balance balanceForCurrency = balancesForExchange.get(currency);
				BigDecimal currentBalance = balanceForCurrency == null ? BigDecimal.ZERO : balanceForCurrency.getAvailable();
				BigDecimal lastBalancedAmount = walletService.getLastBalancedAmount(toExchangeName, currencyCode);
				BigDecimal checkThresholdBalance = toWallet.getInitialBalance().max(lastBalancedAmount).multiply(new BigDecimal(balanceCheckThreshold));
				LOG.debug("# Exchange : " + toExchangeName + " -> " + currency.getDisplayName() + " : currentBalance = " + currentBalance + " / checkThresholdBalance = " + checkThresholdBalance);

				// trigger the balancer
				if (currentBalance.compareTo(checkThresholdBalance) < 0) {
					LOG.info("### Exchange needs to be balanced for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					Exchange fromExchange = findMostFilledBalance(exchangeMap.keySet(), balanceMap, walletMap, currency);
					if (fromExchange == null) {
						LOG.error("Balancing error : All the accounts balances are zero");
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					if (fromExchange.equals(toExchange)) {
						LOG.error("Balancing error : The source and the destination accounts are identical");
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
					Map<String, MyWallet> fromExchangeWallets = walletMap.get(fromExchangeName);
					if (fromExchangeWallets == null) {
						LOG.warn("Configuration error : Missing wallet config for source account : " + fromExchangeName);
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					MyWallet fromWallet = fromExchangeWallets.get(currencyCode);
					if (fromWallet == null) {
						LOG.warn("Configuration error : Missing wallet config for source account for currency : " + fromExchangeName + " -> " + currency.getDisplayName());
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					try {
						// the rebalancing actually occured
						if (balance(fromExchange, toExchange, balanceMap.get(fromExchange).get(currency), balanceForCurrency, currency, fromWallet, toWallet)) {
							BigDecimal newDecreasedBalance = walletService.getAvailableBalance(fromExchange, exchangeMap.get(fromExchange), currency);
							LOG.info("new provisional balance for " + fromExchangeName + " -> " + currency.getDisplayName() + " : " + newDecreasedBalance);
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
	
	private TradeHistoryParams getTradeHistoryParams(Exchange exchange, Currency currency) {
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		// TODO to check that code
		if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
			HitbtcFundingHistoryParams.Builder builder = new HitbtcFundingHistoryParams.Builder();
			return builder.currency(currency).offset(0).limit(100).build();
		} else {
			return new DefaultTradeHistoryParamCurrency(currency);
		}
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
					LOG.info("Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " ["
							+ exchangeName + " -> " + currency.getDisplayName() + "] amount: " + amountDeducted);
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
			LOG.info("Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " [" + exchangeName + " -> " + currency.getDisplayName() + "] amount: " + amountToWithdraw);
			if (Currency.XRP.equals(currency)) {
				return exchange.getAccountService().withdrawFunds(new RippleWithdrawFundsParams(withdrawAddress, currency, amountToWithdraw, paymentId));
			}
			else {
				return exchange.getAccountService().withdrawFunds(currency, amountToWithdraw, withdrawAddress);
			}
		}
	}
	
	private boolean balance(Exchange fromExchange, Exchange toExchange, Balance fromBalance, Balance toBalance, Currency currency, MyWallet fromWallet, MyWallet toWallet) 
			throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException {
		String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
		String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
		
		LOG.info("Source exchange [" + fromExchangeName + " -> " + currency.getDisplayName() + "] balance : "
				+ fromBalance + " / Destination exchange [" + toExchangeName + " -> "
				+ currency.getDisplayName() + "] balance : " + toBalance);
		
		BigDecimal balancedOffset = fromBalance.getAvailable().subtract(toBalance.getAvailable()).divide(BigDecimal.valueOf(2));
		BigDecimal allowedWithdrawableAmount = fromBalance.getAvailable().subtract(fromWallet.getMinResidualBalance());
		// we add the withdrawal fee to the amount to withdraw as it will be deducted automatically in order to get the desired withdrawal amount
		BigDecimal amountToWithdraw = transxService.roundAmount(balancedOffset.min(allowedWithdrawableAmount), currency).add(fromWallet.getWithdrawalFee());
		LOG.debug("### amountToWithdraw = min (balancedOffset, allowedWithdrawableAmount) + withdrawalFee = min ("
				+ balancedOffset + ", " + allowedWithdrawableAmount + ") + " + fromWallet.getWithdrawalFee());
		
		// amountToWithdraw must be higher than the minimum amount specified in the config
		if (amountToWithdraw.compareTo(fromWallet.getMinWithdrawalAmount()) < 0) {
			LOG.error("Withdraw amount is too low - lower than minWithdrawAmount = " + fromWallet.getMinWithdrawalAmount() + " for " + fromExchangeName + " -> " + currency.getDisplayName());
			return false;
		}
		
		// if fees > 0.05% x amountToWithdraw TODO disable for ETH/BTC
		double percent = 0.05 / 100;
		BigDecimal fees = fromWallet.getWithdrawalFee().add(toWallet.getDepositFee());
/*		BigDecimal percentOfAmount = BigDecimal.valueOf(percent).multiply(amountToWithdraw);
		if (fees.compareTo(percentOfAmount) > 0) {
			LOG.error("Withdraw not authorized : fees are too high!");
			LOG.debug("fees > percentOfAmount : " + fees + " > " + percentOfAmount);
			return false;
		}*/
		
		LOG.info("### amountToWithdraw [" + fromExchangeName + " -> " + toExchangeName + "] : " + amountToWithdraw);
		
 		String depositAddress = walletService.getDepositAddress(toExchange, toExchangeName, toWallet, currency);
 		if (depositAddress == null) {
			LOG.error("Unexpected error : The deposit address of the destination wallet is null");
			LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
			return false;
 		}
		LOG.debug("Deposit address [" + toExchangeName + " -> " + currency.getDisplayName() + "] : " + depositAddress);

		if (!simulate) {
			String internalId = withdrawFunds(fromExchange, depositAddress, currency, amountToWithdraw, toWallet.getTag(), fees);
			LOG.debug("internalId = " + internalId);
			
			// load the history in order to retrieve the matching externalId (transactionId) from the internalId returned by the withdrawFunds method
			Integer transxHashkey = -1;
			int numAttempts = 0;
			do {
				try {
					LOG.debug("Waiting for the transactionId... sleeping for (ms) : " + withdrawalWaitingDelay);
					Thread.sleep(withdrawalWaitingDelay);
					
					List<FundingRecord> fundingRecords = fromExchange.getAccountService().getFundingHistory(getTradeHistoryParams(fromExchange, currency));	
					Optional<FundingRecord> matchingFundingRecord = transxService.filterByInternalId(fundingRecords, internalId);
					if (matchingFundingRecord.isPresent()){
						// the amount that will be deposited in the target account will be deducted from the deposit fee
						BigDecimal depositAmount = transxService.roundAmount(matchingFundingRecord.get().getAmount(), matchingFundingRecord.get().getCurrency()).subtract(toWallet.getDepositFee());
						transxHashkey = transxService.transxHashkey(matchingFundingRecord.get().getCurrency(), depositAmount, depositAddress);
						LOG.debug("### transxHashkey : " + transxHashkey + " = (" + matchingFundingRecord.get().getCurrency() + ", " + depositAmount + ", " + depositAddress + ")");
					}
					numAttempts++;
					if (numAttempts == 10) {
						throw new Exception("Maximum number of attempts reached: " + numAttempts);
					}
				} catch (HttpStatusIOException hse) {
					LOG.error("Exchange exception : ", hse);
					
				} catch (Exception e) {
					LOG.fatal("Unexpected error : Cannot monitor pending withdrawal for " + fromExchangeName + " -> " + currency.getDisplayName() + " with transactionId = " + internalId);
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
				nbOperations = balanceAccounts(exchangeMap, currencies, wallets);
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

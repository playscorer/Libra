package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
import org.knowm.xchange.service.trade.params.DefaultWithdrawFundsParams;
import org.knowm.xchange.service.trade.params.RippleWithdrawFundsParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.WithdrawFundsParams;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.Wallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.WalletService;
import arbitrail.libra.orm.spring.ContextProvider;
import arbitrail.libra.utils.Utils;
import si.mazi.rescu.HttpStatusIOException;

public class BalancerServiceImpl implements BalancerService {
	
	private final static Logger LOG = Logger.getLogger(BalancerServiceImpl.class);
	
	private int withdrawalWaitingDelay = 30000;
	private WalletService walletService = ContextProvider.getBean(WalletService.class);
	private TransactionService transxService = new TransactionServiceImpl();
	private Double balanceCheckThreshold;
	private Boolean simulate;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	
	public BalancerServiceImpl(Properties properties, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap) {
		balanceCheckThreshold = Double.valueOf(properties.getProperty(Utils.Props.balance_check_threshold.name()));
		simulate = Boolean.valueOf(properties.getProperty(Utils.Props.simulate.name()));
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
	}


	
	private Exchange findMostFilledBalance(List<Exchange> exchangeList, Map<String, Map<String, Wallet>> walletMap, Currency currency) throws IOException {
		Exchange maxExchange = null;
		BigDecimal maxBalance = BigDecimal.ZERO;
		
		for (Exchange exchange : exchangeList) {
			Balance balance = walletService.getBalance(walletService.getWallet(exchange, walletMap.get(exchange.getExchangeSpecification().getExchangeName()).get(currency.getCurrencyCode())), currency);
			if (balance == null) 
				continue;
			if (maxBalance.compareTo(balance.getAvailable()) < 0) {
				maxExchange = exchange;
				maxBalance = balance.getAvailable();
			}
		}
		
		return maxExchange;
	}
	
	@Override
	public int balanceAccounts(List<Exchange> exchangeList, List<Currency> currencyList, Wallets wallets) throws IOException, InterruptedException {
		Map<String, Map<String, Wallet>> walletMap = wallets.getWalletMap();
		int nbOperations = 0;
		
		for (Exchange toExchange : exchangeList) {
			String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
			Map<String, Wallet> toExchangeWallets = walletMap.get(toExchangeName);
			// no currencies set up for the exchange
			if (toExchangeWallets == null) {
				LOG.warn("No wallet config found for destination account : " + toExchangeName);
				LOG.info("Skipping exchange : " + toExchangeName);
				continue;
			}
			
			for (Currency currency : currencyList) {
				String currencyCode = currency.getCurrencyCode();
				Wallet toWallet = toExchangeWallets.get(currencyCode);
				// this currency is not set up for the exchange
				if (toWallet == null) {
					LOG.warn("No wallet config found for destination account for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}

				// do the rebalancing only if there is no pending withdrawal
				Boolean pendingWithdrawal = pendingWithdrawalsMap.get(new ExchCcy(toExchangeName, currencyCode));
				if (pendingWithdrawal != null && pendingWithdrawal) {
					LOG.debug("Pending withdrawal : Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}
				
				// there has been no rebalancing yet and the pending service may not be running
				else if (pendingWithdrawal == null) {
					LOG.debug("No balancing has been done yet for " + toExchangeName + " -> " + currency.getDisplayName());
				}				
				
				// the threshold represents the minimum amount from which the balance will be triggered
				Balance currentBalance = walletService.getBalance(walletService.getWallet(toExchange, toWallet), currency);
				if (currentBalance == null) {
					LOG.info("Source exchange [" + toExchangeName + " -> " + currency.getDisplayName() + "] balance unavailable ");
					continue;
				}
				BigDecimal lastBalancedAmount = walletService.getLastBalancedAmount(toExchangeName, currencyCode);
				BigDecimal checkThresholdBalance = toWallet.getInitialBalance().max(lastBalancedAmount).multiply(new BigDecimal(balanceCheckThreshold));
				LOG.debug("Exchange : " + toExchangeName + " -> " + currency.getDisplayName() + " / checkThresholdBalance = " + checkThresholdBalance + " / currentBalance = " + currentBalance.getAvailable());

				// trigger the balancer
				if (currentBalance.getAvailable().compareTo(checkThresholdBalance) < 0) {
					LOG.info("### Exchange needs to be balanced for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					Exchange fromExchange = findMostFilledBalance(exchangeList, walletMap, currency);
					if (fromExchange == null) {
						LOG.error("Unexpected error : All the accounts balances are zero");
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
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
					Wallet fromWallet = fromExchangeWallets.get(currencyCode);
					if (fromWallet == null) {
						LOG.error("Unexpected error : Missing wallet config for source account for currency : " + fromExchangeName + " -> " + currency.getDisplayName());
						LOG.info("Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					try {
						// the rebalancing actually occured
						if (balance(fromExchange, toExchange, currency, fromWallet, toWallet)) {
							Balance newDecreasedBalance = walletService.getWallet(fromExchange, fromWallet).getBalance(currency);
							LOG.info("new provisional balance for " + fromExchangeName + " -> " + currency.getDisplayName() + " : " + newDecreasedBalance.getAvailable());
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
	
	private TradeHistoryParams getTradeHistoryParams(Exchange exchange, Currency currency)
	{
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		if (exchangeName.equals("Hitbtc")) {
			HitbtcFundingHistoryParams.Builder builder = new HitbtcFundingHistoryParams.Builder();
			return builder.currency(currency).offset(0).limit(100).build();
		}
		else
			return new DefaultTradeHistoryParamCurrency(currency);
	}	
	
	private String withdrawFunds(Exchange exchange, String withdrawAddress, Currency currency, BigDecimal amountToWithdraw, String paymentId, BigDecimal fee) throws IOException, InterruptedException {
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		if (exchangeName.equals("Hitbtc")) {
			HitbtcAccountService hitbtcAccountService = (HitbtcAccountService)exchange.getAccountService();
			hitbtcAccountService.transferToMain(currency, amountToWithdraw);
			int nbTry = 3;
			while(--nbTry > 0)
			{
				try {
					LOG.info("Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " [" + exchangeName + " -> " + currency.getDisplayName() + "] amount: " + amountToWithdraw);
					return hitbtcAccountService.withdrawFundsRaw(currency, amountToWithdraw.subtract(fee), withdrawAddress, paymentId);
				}
				catch(HttpStatusIOException exc) {
					if (nbTry == 1){
						// revert the withdraw
						hitbtcAccountService.transferToTrading(currency, amountToWithdraw);
						throw exc;
					}
					Thread.sleep(2000);
				}
			}
			return null;
		}
		else {
			LOG.info("Sending withdraw order - address: " + withdrawAddress + " id: " + paymentId + " [" + exchangeName + " -> " + currency.getDisplayName() + "] amount: " + amountToWithdraw);
			if (Currency.XRP.equals(currency)) {
				WithdrawFundsParams withdrawParams = new RippleWithdrawFundsParams(withdrawAddress, currency, amountToWithdraw.subtract(fee), paymentId);
				return exchange.getAccountService().withdrawFunds(withdrawParams);
			}
			else
				return exchange.getAccountService().withdrawFunds(currency, amountToWithdraw.subtract(fee), withdrawAddress);
		}
	}

	private boolean balance(Exchange fromExchange, Exchange toExchange, Currency currency, Wallet fromWallet, Wallet toWallet) 
			throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException {
		String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
		String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
		
		Balance toBalance = walletService.getBalance(walletService.getWallet(toExchange, toWallet), currency);
		if (toBalance == null) {
			LOG.info("Source exchange [" + toExchangeName + " -> " + currency.getDisplayName() + "] balance unavailable ");
			return false;
		}
		Balance fromBalance = walletService.getBalance(walletService.getWallet(fromExchange, fromWallet), currency);
		if (fromBalance == null) {
			LOG.info("Source exchange [" + fromExchangeName + " -> " + currency.getDisplayName() + "] balance unavailable ");
			return false;
		}
		LOG.info("Source exchange [" + fromExchangeName + " -> " + currency.getDisplayName() + "] balance : "
				+ fromBalance.getAvailable() + " / Destination exchange [" + toExchangeName + " -> "
				+ currency.getDisplayName() + "] balance : " + toBalance.getAvailable());
		
		BigDecimal balancedOffset = fromBalance.getAvailable().subtract(toBalance.getAvailable()).divide(BigDecimal.valueOf(2));
		BigDecimal allowedWithdrawableAmount = fromBalance.getAvailable().subtract(fromWallet.getMinResidualBalance());
		BigDecimal amountToWithdraw = transxService.roundAmount(balancedOffset.min(allowedWithdrawableAmount), currency).add(fromWallet.getWithdrawalFee());
		LOG.debug("amountToWithdraw = min (balancedOffset, allowedWithdrawableAmount) = min (" + balancedOffset + ", " + allowedWithdrawableAmount + ")");
		
		// amountToWithdraw must be higher than the minimum amount specified in the config
		if (amountToWithdraw.compareTo(fromWallet.getMinWithdrawalAmount()) < 0) {
			LOG.error("Withdraw amount is too low - lower than minWithdrawAmount = " + fromWallet.getMinWithdrawalAmount() + " for " + fromExchangeName + " -> " + currency.getDisplayName());
			return false;
		}
		
		// if fees > 0.05% x amountToWithdraw
		double percent = 0.05 / 100;
		BigDecimal fees = fromWallet.getWithdrawalFee().add(toWallet.getDepositFee());
		BigDecimal percentOfAmount = BigDecimal.valueOf(percent).multiply(amountToWithdraw);
		if (fees.compareTo(percentOfAmount) > 0) {
			LOG.error("Withdraw not authorized : fees are too high!");
			LOG.debug("fees / percentOfAmount : " + fees + " / " + percentOfAmount);
			return false;
		}
		
		LOG.debug("amountToWithdraw [" + fromExchangeName + " -> " + toExchangeName + "] : " + amountToWithdraw);
		
 		String depositAddress = toExchange.getAccountService().requestDepositAddress(currency);
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
					
					TradeHistoryParams ccyHistoryParams = getTradeHistoryParams(fromExchange, currency);
					List<FundingRecord> fundingRecords = fromExchange.getAccountService().getFundingHistory(ccyHistoryParams);	
					Optional<FundingRecord> matchingFundingRecord = transxService.retrieveExternalId(fundingRecords, internalId);
					if (matchingFundingRecord.isPresent()){
						BigDecimal roundedAmount = transxService.roundAmount(matchingFundingRecord.get().getAmount(), matchingFundingRecord.get().getCurrency());
						transxHashkey = transxService.transxHashkey(matchingFundingRecord.get().getCurrency(), roundedAmount, depositAddress);
					}
					numAttempts++;
					if (numAttempts == 10)
						throw new Exception("Maximum number of attempts reached: " + numAttempts);
				} catch (Exception e) {
					LOG.fatal("Unexpected error : Cannot monitor pending withdrawal for " + fromExchangeName + " -> " + currency.getDisplayName() + " with transactionId = " + internalId);
					LOG.fatal("Exception : " + e);
					LOG.fatal("Libra has stopped!");
					System.exit(-1);
				}
			} while (transxHashkey == -1);
			
			// set map pending transactions to true
			ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
			pendingWithdrawalsMap.put(exchCcy, true);

			// add mapping destination exchange to transactionId
			ExchStatus exchStatus = new ExchStatus(toExchangeName, false, Calendar.getInstance().getTime());
			transxIdToTargetExchMap.put(transxHashkey, exchStatus);
			
			return true;
		}
		
		return false;
	}

}

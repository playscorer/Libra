package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.RippleWithdrawFundsParams;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.Wallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.WalletService;
import arbitrail.libra.orm.spring.ContextProvider;
import arbitrail.libra.utils.Utils;

public class BalancerServiceImpl implements BalancerService {
	
	private final static Logger LOG = Logger.getLogger(BalancerServiceImpl.class);
	
	private int withdrawalWaitingDelay = 30000;
	private WalletService walletService = ContextProvider.getBean(WalletService.class);
	private TransactionService transxService = new TransactionServiceImpl();
	private Double balanceCheckThreshold;
	private Boolean simulate;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap;
	
	public BalancerServiceImpl(Properties properties, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap) {
		balanceCheckThreshold = Double.valueOf(properties.getProperty(Utils.Props.balance_check_threshold.name()));
		simulate = Boolean.valueOf(properties.getProperty(Utils.Props.simulate.name()));
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
	}


	
	private Exchange findMostFilledBalance(List<Exchange> exchangeList, Currency currency) throws IOException {
		Exchange maxExchange = null;
		BigDecimal maxBalance = BigDecimal.ZERO;
		
		for (Exchange exchange : exchangeList) {
			Balance balance = exchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
			if (maxBalance.compareTo(balance.getAvailable()) < 0) {
				maxExchange = exchange;
				maxBalance = balance.getAvailable();
			}
		}
		
		return maxExchange;
	}
	
	@Override
	public int balanceAccounts(List<Exchange> exchangeList, List<Currency> currencyList, Wallets wallets) throws IOException {
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
					LOG.info("Pending withdrawal : Skipping currency for exchange : " + toExchangeName + " -> " + currency.getDisplayName());
					continue;
				}
				
				// there has been no rebalancing yet and the pending service may not be running
				else if (pendingWithdrawal == null) {
					LOG.info("No balancing has been done yet for " + toExchangeName + " -> " + currency.getDisplayName());
				}
				
				// the threshold represents the minimum amount from which the balance will be triggered
				Balance currentBalance = toExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
				BigDecimal lastBalancedAmount = walletService.getLastBalancedAmount(toExchangeName, currencyCode);
				BigDecimal checkThresholdBalance = toWallet.getInitialBalance().max(lastBalancedAmount).multiply(new BigDecimal(balanceCheckThreshold));
				LOG.debug("Exchange : " + toExchangeName + " -> " + currency.getDisplayName() + " / checkThresholdBalance = " + checkThresholdBalance + " / currentBalance = " + currentBalance.getAvailable());

				// trigger the balancer
				if (currentBalance.getAvailable().compareTo(checkThresholdBalance) < 0) {
					LOG.info("### Exchange needs to be balanced for currency : " + toExchangeName + " -> " + currency.getDisplayName());
					Exchange fromExchange = findMostFilledBalance(exchangeList, currency);
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
							Balance newDecreasedBalance = fromExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
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

	private boolean balance(Exchange fromExchange, Exchange toExchange, Currency currency, Wallet fromWallet, Wallet toWallet) 
			throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
		String toExchangeName = toExchange.getExchangeSpecification().getExchangeName();
		String fromExchangeName = fromExchange.getExchangeSpecification().getExchangeName();
		
		Balance toBalance = toExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
		Balance fromBalance = fromExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
		LOG.info("Source exchange [" + fromExchangeName + " -> " + currency.getDisplayName() + "] balance : "
				+ fromBalance.getAvailable() + " / Destination exchange [" + toExchangeName + " -> "
				+ currency.getDisplayName() + "] balance : " + toBalance.getAvailable());
		
		BigDecimal balancedOffset = fromBalance.getAvailable().subtract(toBalance.getAvailable()).divide(BigDecimal.valueOf(2));
		BigDecimal allowedWithdrawableAmount = fromBalance.getAvailable().subtract(fromWallet.getMinResidualBalance());
		BigDecimal amountToWithdraw = balancedOffset.min(allowedWithdrawableAmount);
		LOG.debug("amountToWithdraw = min (balancedOffset, allowedWithdrawableAmount) = min (" + balancedOffset + ", " + allowedWithdrawableAmount + ")");
		
		// amountToWithdraw cannot be negative
		if (BigDecimal.ZERO.compareTo(amountToWithdraw) >= 0) {
			LOG.error("Withdraw amount can't be negative or 0 - please decrease the minResidualBalance = " + fromWallet.getMinResidualBalance() + " for " + fromExchangeName + " -> " + currency.getDisplayName());
			return false;
		}
		
		// if fees > 0.5% x amountToWithdraw
		double percent = 0.5 / 100;
		BigDecimal fees = fromWallet.getWithdrawalFee().add(toWallet.getDepositFee());
		BigDecimal percentOfAmount = BigDecimal.valueOf(percent).multiply(amountToWithdraw);
		if (fees.compareTo(percentOfAmount) > 0) {
			LOG.error("Withdraw not authorized : fees are too high!");
			LOG.debug("fees / percentOfAmount : " + fees + " / " + percentOfAmount);
			return false;
		}
		
		LOG.info("amountToWithdraw [" + fromExchangeName + " -> " + toExchangeName + "] : " + amountToWithdraw);
		
 		String depositAddress = toExchange.getAccountService().requestDepositAddress(currency);
		LOG.debug("Deposit address [" + toExchangeName + " -> " + currency.getDisplayName() + "] : " + depositAddress);

		if (!simulate) {
			String internalId;
			if (Currency.XRP.equals(currency)) {
				internalId = fromExchange.getAccountService().withdrawFunds(new RippleWithdrawFundsParams(depositAddress, currency, amountToWithdraw, toWallet.getTag()));
			} else {
				internalId = fromExchange.getAccountService().withdrawFunds(currency, amountToWithdraw, depositAddress);
			}
			LOG.debug("internalId = " + internalId);
			
			// load the history in order to retrieve the matching externalId (transactionId) from the internalId returned by the withdrawFunds method
			Optional<FundingRecord> matchingFundingRecord;
			do {
				try {
					LOG.debug("Waiting for the transactionId... sleeping for (ms) : " + withdrawalWaitingDelay);
					Thread.sleep(withdrawalWaitingDelay);
				} catch (InterruptedException e) {
					LOG.error("Unexpected error : " + e);
				}
				List<FundingRecord> fundingRecords = fromExchange.getAccountService().getFundingHistory(new DefaultTradeHistoryParamCurrency(currency));	
				matchingFundingRecord = transxService.retrieveExternalId(fundingRecords, internalId);
			} while (!matchingFundingRecord.isPresent());
			
			String externalId = matchingFundingRecord.get().getExternalId();
			if (externalId == null) {//TODO Bitstamp not supported at the moment
				LOG.fatal("Unexpected error : Cannot monitor pending withdrawal for " + fromExchangeName + " -> " + currency.getDisplayName() + " with transactionId = " + internalId);
				LOG.fatal("Libra has stopped!");
				System.exit(-1);
			}
			
			// set map pending transactions to true
			ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
			pendingWithdrawalsMap.put(exchCcy, true);

			// add mapping destination exchange to transactionId
			ExchStatus exchStatus = new ExchStatus(toExchangeName, false);
			transxIdToTargetExchMap.put(externalId, exchStatus);
			
			return true;
		}
		
		return false;
	}

}

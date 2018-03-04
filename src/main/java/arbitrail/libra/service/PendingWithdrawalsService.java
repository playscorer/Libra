package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.FundingRecord.Status;
import org.knowm.xchange.dto.account.FundingRecord.Type;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.knowm.xchange.hitbtc.v2.service.HitbtcFundingHistoryParams;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.model.WalletEntity;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.orm.service.WalletService;
import arbitrail.libra.orm.spring.ContextProvider;

public class PendingWithdrawalsService extends Thread {
	
	private final static Logger LOG = Logger.getLogger(PendingWithdrawalsService.class);
	
	private TransxIdToTargetExchService transxIdToTargetExchService = ContextProvider.getBean(TransxIdToTargetExchService.class);
	private PendingTransxService pendingTransxService = ContextProvider.getBean(PendingTransxService.class);
	private WalletService walletService = ContextProvider.getBean(WalletService.class);
	private TransactionService transxService = new TransactionServiceImpl();
	
	private List<Exchange> exchanges;
	private Wallets wallets;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	private Integer frequency;
	
	public PendingWithdrawalsService(List<Exchange> exchanges, Wallets wallets, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, Integer frequency) {
		this.exchanges = exchanges;
		this.wallets = wallets;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.frequency = frequency;
	}
	
	private TradeHistoryParams getTradeHistoryParams(Exchange exchange)
	{
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		if (exchangeName.equals("Hitbtc")) {
			HitbtcFundingHistoryParams.Builder builder = new HitbtcFundingHistoryParams.Builder();
			return builder.offset(0).limit(100).build();
		}
		else if (exchangeName.equals("BitFinex")) {
			// TODO: Support multi-currency TradeHistoryParams. in the meantime we return the first on the list
			for (String currencyName : wallets.getWalletMap().get(exchangeName).keySet())
				return new DefaultTradeHistoryParamCurrency(new Currency(currencyName));
			return null;
		}
		else
			return new DefaultTradeHistoryParamCurrency();
	}

	public void pollPendingWithdrawals() {
		for (Exchange exchange : exchanges) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();

			try {
				TradeHistoryParams ccyHistoryParams = getTradeHistoryParams(exchange);
				List<FundingRecord> fundingRecords = exchange.getAccountService().getFundingHistory(ccyHistoryParams);
				fundingRecords = transxService.retrieveLastTwoDaysOf(fundingRecords);
				//LOG.debug("FundingRecords for exchange " + exchangeName + " :");
				//LOG.debug(fundingRecords);
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					//LOG.debug("xchg: " + exchangeName + ". ccy: " + fundingRecord.getCurrency() + ". amount: " + fundingRecord.getAmount());
					Integer transxHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), fundingRecord.getAmount(), fundingRecord.getAddress());
					if (transxHashkey == null) {
						LOG.error("Unexpected error : transxHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}					
					// check if the transactions are part of recent transactions handled by Libra
					Currency currency = fundingRecord.getCurrency();
					if (transxIdToTargetExchMap.keySet().contains(transxHashkey)) {							
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							ExchStatus exchStatus = transxIdToTargetExchMap.get(transxHashkey);
							if (exchStatus == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name");
								LOG.warn("Skipping update of pending withdrawals status from exchange : " + exchangeName + " -> " + currency.getDisplayName());
								continue;
							}
							// filter trades recorded before the withdrawal
							if (!exchStatus.isLive(fundingRecord.getDate())) {
								LOG.warn("Filtered a withdraw : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
								continue;
							}
							LOG.warn("Detected a withdraw : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
							String toExchangeName = exchStatus.getExchangeName();
							ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
							
							// pending withdrawals
							if (Status.PROCESSING.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, true);
							}
							// withdrawals cancelled or failed
							else if (Status.CANCELLED.equals(fundingRecord.getStatus()) || Status.FAILED.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, false); 
								transxIdToTargetExchMap.remove(transxHashkey);
							}
							// withdrawals completed
							else if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								// first check
								if (!exchStatus.isWithdrawalComplete()) {
									exchStatus.setWithdrawalComplete(true);
									saveUpdatedBalance(exchange, exchangeName, wallets.getWalletMap().get(exchangeName).get(currency.getCurrencyCode()), currency);
								}
							}
						}
					}						
					// filter completed deposits
					BigDecimal roundedAmount = transxService.roundAmount(fundingRecord.getAmount(), fundingRecord.getCurrency());
					Integer depositHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), roundedAmount, exchange.getAccountService().requestDepositAddress(currency));
					if (depositHashkey == null) {
						LOG.error("Unexpected error : depositHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}
					if (transxIdToTargetExchMap.keySet().contains(depositHashkey)) {
						if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							// filter trades recorded before the withdrawal
							if (!transxIdToTargetExchMap.get(depositHashkey).isLive(fundingRecord.getDate())) {
								LOG.warn("Filtered a deposit : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
								continue;
							}
							LOG.warn("Detected a deposit : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
							if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								ExchCcy exchCcy = new ExchCcy(exchangeName, currency.getCurrencyCode());
								pendingWithdrawalsMap.put(exchCcy, false); 
								transxIdToTargetExchMap.remove(depositHashkey);
								saveUpdatedBalance(exchange, exchangeName, wallets.getWalletMap().get(exchangeName).get(currency.getCurrencyCode()), currency);
								if (exchangeName.equals("Hitbtc")) {
									HitbtcAccountService hitbtcAccountService = (HitbtcAccountService)exchange.getAccountService();
									hitbtcAccountService.transferToTrading(currency, fundingRecord.getAmount());
								}
							}
						}
					}
				}
			} catch (IOException e) {
				LOG.error("Unexpected error when retrieving funding history : " + e);
				LOG.warn("Skipping polling withdrawals / deposits status for exchange : " + exchangeName);
				continue;
			}
		}
	}

	private void saveUpdatedBalance(Exchange exchange, String exchangeName, arbitrail.libra.model.Wallet wallet, Currency currency) throws IOException {
		Balance newBalance = walletService.getBalance(walletService.getWallet(exchange, wallet), currency);
		if (newBalance != null)
			LOG.info("newBalance for " + exchangeName + " -> " + currency.getDisplayName() + " : " + newBalance.getAvailable());
		else
			LOG.error("cannot save balance for " + exchangeName + " -> " + currency.getDisplayName() + " : balance unavailable");
		// DISABLED TEMPORARILY, crashes with "Exception in thread "Thread-0" javax.persistence.TransactionRequiredException: No EntityManager with actual transaction available for current thread - cannot reliably process 'persist' call"
		//WalletEntity walletEntity = new WalletEntity(exchangeName, currency.getCurrencyCode(), newBalance.getAvailable());
		//walletService.save(walletEntity); 
	}

	@Override
	public void run() {
		LOG.info("PendingWithdrawals service has started!");
		while (true) {
			try {
				pollPendingWithdrawals();
				
				// DISABLED TEMPORARILY, crashes with "Exception in thread "Thread-0" javax.persistence.TransactionRequiredException: No EntityManager with actual transaction available for current thread - cannot reliably process 'persist' call"
				//LOG.debug("Persisting the transaction Ids : " + transxIdToTargetExchMap);
				//transxIdToTargetExchService.saveAll(transxIdToTargetExchMap);
				
				//LOG.debug("Persisting the status of the pending transactions : " + pendingWithdrawalsMap);
				//pendingTransxService.saveAll(pendingWithdrawalsMap);
				
				LOG.debug("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}

}

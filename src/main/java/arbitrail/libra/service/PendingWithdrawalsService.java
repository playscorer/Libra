package arbitrail.libra.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.FundingRecord.Status;
import org.knowm.xchange.dto.account.FundingRecord.Type;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
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
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap;
	private Integer frequency;
	
	public PendingWithdrawalsService(List<Exchange> exchanges, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap, Integer frequency) {
		this.exchanges = exchanges;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.frequency = frequency;
	}

	public void pollPendingWithdrawals() {
		for (Exchange exchange : exchanges) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();

			try {
				List<FundingRecord> fundingRecords = exchange.getAccountService().getFundingHistory(new DefaultTradeHistoryParamCurrency());
				fundingRecords = transxService.retrieveLastTwoDaysOf(fundingRecords);
				//LOG.debug("FundingRecords for exchange " + exchangeName + " :");
				//LOG.debug(fundingRecords);
				
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					String externalId = fundingRecord.getExternalId();
					if (externalId == null) {
						LOG.error("This exchange does not handle the externalId : " + exchangeName);
						continue;
					}
					//TODO useful for Bitstamp
/*					Integer transxHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), fundingRecord.getAmount(), fundingRecord.getAddress());
					if (transxHashkey == null) {
						LOG.error("Unexpected error : transxHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}*/ 
					
					// check if the transactions are part of recent transactions handled by Libra
					if (transxIdToTargetExchMap.keySet().contains(externalId)) {
						Currency currency = fundingRecord.getCurrency();
						
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							ExchStatus exchStatus = transxIdToTargetExchMap.get(externalId);
							if (exchStatus == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name : " + externalId);
								LOG.warn("Skipping update of pending withdrawals status from exchange : " + exchangeName + " -> " + currency.getDisplayName());
								continue;
							}
							String toExchangeName = exchStatus.getExchangeName();
							ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
							
							// pending withdrawals
							if (Status.PROCESSING.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, true);
							}
							// withdrawals cancelled or failed
							else if (Status.CANCELLED.equals(fundingRecord.getStatus()) || Status.FAILED.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, false); 
								transxIdToTargetExchMap.remove(externalId);
							}
							// withdrawals completed
							else if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								// first check
								if (!exchStatus.isWithdrawalComplete()) {
									exchStatus.setWithdrawalComplete(true);
									saveUpdatedBalance(exchange, exchangeName, currency);
								}
							}
						}
						
						// filter completed deposits
						else if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								ExchCcy exchCcy = new ExchCcy(exchangeName, currency.getCurrencyCode());
								pendingWithdrawalsMap.put(exchCcy, false); 
								transxIdToTargetExchMap.remove(externalId);
								saveUpdatedBalance(exchange, exchangeName, currency);
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

	private void saveUpdatedBalance(Exchange exchange, String exchangeName, Currency currency) throws IOException {
		Balance newBalance = exchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
		LOG.info("newBalance for " + exchangeName + " -> " + currency.getDisplayName() + " : " + newBalance.getAvailable());
		WalletEntity wallet = new WalletEntity(exchangeName, currency.getCurrencyCode(), newBalance.getAvailable());
		walletService.save(wallet); 
	}

	@Override
	public void run() {
		LOG.info("PendingWithdrawals service has started!");
		while (true) {
			try {
				pollPendingWithdrawals();
				
				LOG.debug("Persisting the transaction Ids : " + transxIdToTargetExchMap);
				transxIdToTargetExchService.saveAll(transxIdToTargetExchMap);
				
				LOG.debug("Persisting the status of the pending transactions : " + pendingWithdrawalsMap);
				pendingTransxService.saveAll(pendingWithdrawalsMap);
				
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}

}

package arbitrail.libra.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.FundingRecord.Status;
import org.knowm.xchange.dto.account.FundingRecord.Type;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.spring.ContextProvider;

public class PendingWithdrawalsService extends Thread {
	
	private final static Logger LOG = Logger.getLogger(PendingWithdrawalsService.class);
	
	private PendingTransxService pendingTransxService = ContextProvider.getBean(PendingTransxService.class);
	private TransactionService transxService = new TransactionServiceImpl();
	
	private List<Exchange> exchanges;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<String, String> pendingTransIdToToExchMap;
	private Integer frequency;
	
	public PendingWithdrawalsService(List<Exchange> exchanges, ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<String, String> pendingTransIdToToExchMap, Integer frequency) {
		this.exchanges = exchanges;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
		this.pendingTransIdToToExchMap = pendingTransIdToToExchMap;
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
					if (pendingTransIdToToExchMap.keySet().contains(externalId)) {
						Currency currency = fundingRecord.getCurrency();
						
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							String toExchangeName = pendingTransIdToToExchMap.get(externalId);
							if (toExchangeName == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name : " + externalId);
								LOG.warn("Skipping update of pending withdrawals status from exchange : " + exchangeName + " -> " + currency.getDisplayName());
								continue;
							}
							ExchCcy exchCcy = new ExchCcy(toExchangeName, currency);
							
							// pending withdrawals
							if (Status.PROCESSING.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, true);
							}
							// withdrawals cancelled
							else if (Status.CANCELLED.equals(fundingRecord.getStatus())) {
								pendingWithdrawalsMap.put(exchCcy, false); 
								pendingTransIdToToExchMap.remove(externalId);
							}
						}
						
						// filter completed deposits
						else if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								ExchCcy exchCcy = new ExchCcy(exchangeName, currency);
								pendingWithdrawalsMap.put(exchCcy, false); //TODO update wallets file with balance updated
								pendingTransIdToToExchMap.remove(externalId);
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

	@Override
	public void run() {
		LOG.info("PendingWithdrawals service has started!");
		while (true) {
			try {
				pollPendingWithdrawals();
				LOG.debug(pendingWithdrawalsMap);
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}

}

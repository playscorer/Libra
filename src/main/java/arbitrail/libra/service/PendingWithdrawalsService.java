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
import org.knowm.xchange.service.trade.params.TradeHistoryParams;

import arbitrail.libra.utils.ExchCcy;

public class PendingWithdrawalsService extends Thread {
	
	private final static Logger LOG = Logger.getLogger(PendingWithdrawalsService.class);
	
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
			TradeHistoryParams tradeHistoryParams = exchange.getAccountService().createFundingHistoryParams();

			try {
				List<FundingRecord> fundingRecords = exchange.getAccountService().getFundingHistory(tradeHistoryParams);
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					String transactionId = fundingRecord.getExternalId();
					
					// check if the transactions are part of recent transactions handled by Libra
					if (pendingTransIdToToExchMap.keySet().contains(transactionId)) {
						Currency currency = fundingRecord.getCurrency();
						
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							String toExchangeName = pendingTransIdToToExchMap.get(transactionId);
							if (toExchangeName == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name : " + transactionId);
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
								pendingTransIdToToExchMap.remove(transactionId);
							}
						}
						
						// filter completed deposits
						else if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								ExchCcy exchCcy = new ExchCcy(exchangeName, currency);
								pendingWithdrawalsMap.put(exchCcy, false);
								pendingTransIdToToExchMap.remove(transactionId);
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
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}

}

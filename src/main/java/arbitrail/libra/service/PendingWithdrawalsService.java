package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitstamp.service.BitstampAccountService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.FundingRecord.Status;
import org.knowm.xchange.dto.account.FundingRecord.Type;
import org.knowm.xchange.hitbtc.v2.service.HitbtcAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.MyWallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.model.WalletEntity;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.orm.service.WalletService;

@Component
public class PendingWithdrawalsService extends Thread {
	
	private final static Logger LOG = Logger.getLogger(PendingWithdrawalsService.class);
	
	@Autowired
	private TransxIdToTargetExchService transxIdToTargetExchService;
	
	@Autowired
	private PendingTransxService pendingTransxService;
	
	@Autowired
	private WalletService walletService;
	
	@Autowired
	private TransactionService transxService;
	
	private List<Exchange> exchanges;
	private Wallets wallets;
	private ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	
	@Value("${pending_withdrawals_frequency}")
	private Integer frequency;
	
	public PendingWithdrawalsService() {
		super();
	}
	
	public void init(ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, List<Exchange> exchanges) {
		this.exchanges = exchanges;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
	}

	private void pollPendingWithdrawals() {
		for (Exchange exchange : exchanges) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();

			try {
				List<FundingRecord> fundingRecords = exchange.getAccountService().getFundingHistory(transxService.getTradeHistoryParams(exchange, wallets));
				fundingRecords = transxService.retrieveLastTwoDaysOf(fundingRecords);
				LOG.debug("##################################################################################################################################################");
				LOG.debug("FundingRecords for exchange " + exchangeName + " :");
				LOG.debug(fundingRecords);
				LOG.debug("##################################################################################################################################################");
				
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					// compute hashkey for the withdrawal
					Integer transxHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), fundingRecord.getAmount(), fundingRecord.getAddress());
					if (transxHashkey == null) {
						LOG.error("Unexpected error : transxHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}					
					// check if the transactions are part of recent transactions handled by Libra
					Currency currency = fundingRecord.getCurrency();

					Map<String, MyWallet> walletsForExchange = wallets.getWalletMap().get(exchangeName);
					// no currencies set up for the exchange
					if (walletsForExchange == null) {
						LOG.error("Could not find any wallet configuration for account : " + exchangeName);
						LOG.warn("Skipping exchange : " + exchangeName);
						continue;
					}
					
					MyWallet myWallet = walletsForExchange.get(currency.getCurrencyCode());
					// this currency is not set up for the exchange
					if (myWallet == null) {
						LOG.error("No wallet config found for this account for currency : " + exchangeName + " -> " + currency.getDisplayName());
						LOG.warn("Skipping currency for exchange : " + exchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
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
								// first check to avoid multiple updates of the balance
								if (!exchStatus.isWithdrawalComplete()) {
									exchStatus.setWithdrawalComplete(true);
									saveUpdatedBalance(exchange, exchangeName, myWallet.getLabel(), currency);
								}
							}
						}
					}				
					
					// compute hashkey for the deposit
					String depositAddress;
					BigDecimal roundedAmount = transxService.roundAmount(fundingRecord.getAmount(), fundingRecord.getCurrency());
					// specific case for XRP and Bitstamp TODO
					if (Currency.XRP.equals(fundingRecord.getCurrency()) && ExchangeType.Bitstamp.name().equals(exchangeName)) {
						depositAddress = ((BitstampAccountService) exchange.getAccountService()).getRippleDepositAddress().getAddress();
					} else {
						depositAddress = exchange.getAccountService().requestDepositAddress(currency);
					}
					Integer depositHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), roundedAmount, depositAddress);
					if (depositHashkey == null) {
						LOG.error("Unexpected error : depositHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}
					if (transxIdToTargetExchMap.keySet().contains(depositHashkey)) {
						// filter completed deposits
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
								saveUpdatedBalance(exchange, exchangeName, myWallet.getLabel(), currency);
								//TODO check code
								if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
									HitbtcAccountService hitbtcAccountService = (HitbtcAccountService) exchange.getAccountService();
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

	private void saveUpdatedBalance(Exchange exchange, String exchangeName, String walletId, Currency currency) throws IOException {
		BigDecimal newBalance = walletService.getAvailableBalance(exchange, walletId, currency);
		if (newBalance != null) {
			LOG.info("newBalance for " + exchangeName + " -> " + currency.getDisplayName() + " : " + newBalance);
			WalletEntity walletEntity = new WalletEntity(exchangeName, currency.getCurrencyCode(), newBalance);
			walletService.save(walletEntity);
		} else {
			LOG.error("cannot save balance for " + exchangeName + " -> " + currency.getDisplayName() + " : balance unavailable");
		}
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
				
				LOG.debug("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}
	
	public void setExchanges(List<Exchange> exchanges) {
		this.exchanges = exchanges;
	}

	public void setPendingWithdrawalsMap(ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap) {
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
	}

	public void setTransxIdToTargetExchMap(ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap) {
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
	}

}

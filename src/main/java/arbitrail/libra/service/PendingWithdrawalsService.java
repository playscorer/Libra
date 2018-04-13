package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
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
public class PendingWithdrawalsService implements Runnable {
	
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
	private ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	
	@Value("${pending_withdrawals_frequency}")
	private Integer frequency;
	
	public PendingWithdrawalsService() {
		super();
	}
	
	public void init(Wallets wallets, ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, List<Exchange> exchanges) {
		this.exchanges = exchanges;
		this.wallets = wallets;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
	}

	private void pollPendingWithdrawals() {
		for (Exchange exchange : exchanges) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();

			// loads all wallets for the exchange : needed to get the label and depositAddress for XRP
			Map<String, MyWallet> walletsForExchange = wallets.getWalletMap().get(exchangeName);
			// no currencies set up for the exchange
			if (walletsForExchange == null) {
				LOG.warn("Configuration error : Could not find any wallet configuration for account : " + exchangeName);
				LOG.warn("Skipping exchange : " + exchangeName);
				continue;
			}

			try {
				List<FundingRecord> fundingRecords = exchange.getAccountService().getFundingHistory(transxService.getTradeHistoryParams(exchange, wallets));
				fundingRecords = transxService.retrieveLastTwoDaysOf(fundingRecords);
				LOG.debug("FundingRecords for exchange " + exchangeName + " :");
				LOG.debug("@@ " + fundingRecords);
				
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					// compute hashkey for the withdrawal
					Integer transxHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), fundingRecord.getAmount(), fundingRecord.getAddress());
					LOG.debug("@ Looking for withdrawal transxHashkey : " + transxHashkey + " = (" + fundingRecord.getCurrency() + ", " + fundingRecord.getAmount() + ", " + fundingRecord.getAddress() + ")");
					if (transxHashkey == null) {
						LOG.error("Unexpected error : transxHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}					
					// check if the transactions are part of recent transactions handled by Libra
					Currency currency = fundingRecord.getCurrency();
					
					// loads the wallet for the given Exch/Cur : needed to get the label and depositAddress for XRP
					MyWallet myWallet = walletsForExchange.get(currency.getCurrencyCode());
					// this currency is not set up for the exchange
					if (myWallet == null) {
						LOG.warn("Configuration error : No wallet config found for this account for currency : " + exchangeName + " -> " + currency.getDisplayName());
						LOG.warn("Skipping currency for exchange : " + exchangeName + " -> " + currency.getDisplayName());
						continue;
					}
					
					if (transxIdToTargetExchMap.keySet().contains(transxHashkey)) {
						LOG.debug("@ Withdrawal transxHashkey found in the map of pending transactions : " + transxHashkey);
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							ExchStatus exchStatus = transxIdToTargetExchMap.get(transxHashkey);
							if (exchStatus == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name");
								LOG.warn("Skipping update of pending withdrawals status from exchange : " + exchangeName + " -> " + currency.getDisplayName());
								continue;
							}
							// filter trades recorded before the withdrawal
							if (!exchStatus.isAlive(fundingRecord.getDate())) {
								LOG.warn("Filtered a withdraw : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
								continue;
							}
							LOG.warn("Detected a withdraw : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
							String toExchangeName = exchStatus.getExchangeName();
							ExchCcy exchCcy = new ExchCcy(toExchangeName, currency.getCurrencyCode());
							
							// pending withdrawals
							if (Status.PROCESSING.equals(fundingRecord.getStatus())) {
								LOG.debug("@ Pending withdrawal added to the map of pending withdrawals : " + exchCcy);
								pendingWithdrawalsMap.put(exchCcy, new Object());
								LOG.debug("Saving the pending withdrawal...");
								pendingTransxService.save(exchCcy);
								
							}
							// withdrawals cancelled or failed
							else if (Status.CANCELLED.equals(fundingRecord.getStatus()) || Status.FAILED.equals(fundingRecord.getStatus())) {
								LOG.debug("@ Pending withdrawal removed from the map of pending withdrawals : " + exchCcy);
								pendingWithdrawalsMap.remove(exchCcy); 
								LOG.debug("Deleting the pending withdrawal...");
								pendingTransxService.delete(exchCcy);								
								
								LOG.debug("@ Cancelled/failed withdrawal removed from the map of pending transactions : " + transxHashkey);
								transxIdToTargetExchMap.remove(transxHashkey);
								LOG.debug("Deleting the transaction Id...");
								transxIdToTargetExchService.delete(new AbstractMap.SimpleEntry<Integer, ExchStatus>(transxHashkey, exchStatus));
							}
							// withdrawals completed : only do an update of the balances
							else if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								// first check to avoid multiple updates of the balance
								if (!exchStatus.isWithdrawalComplete()) {
									LOG.debug("@ Pending withdrawal is complete - setting withdrawalComplete boolean to true");
									exchStatus.setWithdrawalComplete(true);
									saveUpdatedBalance(exchange, exchangeName, myWallet.getLabel(), currency);
								}
							}
						}
					}				
					
					// compute hashkey for the deposit
					BigDecimal roundedAmount = transxService.roundAmount(fundingRecord.getAmount(), fundingRecord.getCurrency());
					String depositAddress = walletService.getDepositAddress(exchange, exchangeName, myWallet, currency);
					Integer depositHashkey = transxService.transxHashkey(fundingRecord.getCurrency(), roundedAmount, depositAddress);
					LOG.debug("@ Looking for deposit transxHashkey : " + depositHashkey + " = (" + fundingRecord.getCurrency() + ", " + roundedAmount + ", " + depositAddress + ")");
					if (depositHashkey == null) {
						LOG.error("Unexpected error : depositHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
						continue;
					}
					if (transxIdToTargetExchMap.keySet().contains(depositHashkey)) {
						LOG.debug("@ Deposit transxHashkey found in the map of pending transactions : " + depositHashkey);
						// filter completed deposits
						if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							// filter trades recorded before the deposit
							if (!transxIdToTargetExchMap.get(depositHashkey).isAlive(fundingRecord.getDate())) {
								LOG.warn("Filtered a deposit : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
								continue;
							}
							LOG.warn("Detected a deposit : " + exchangeName + " -> " + fundingRecord.getCurrency().getDisplayName());
							if (Status.COMPLETE.equals(fundingRecord.getStatus())) {
								ExchCcy exchCcy = new ExchCcy(exchangeName, currency.getCurrencyCode());
								LOG.debug("@ Pending withdrawal removed from the map of pending withdrawals : " + exchCcy);
								pendingWithdrawalsMap.remove(exchCcy); 
								LOG.debug("Deleting the pending withdrawal...");
								pendingTransxService.delete(exchCcy);			
								
								LOG.debug("@ Complete withdrawal removed from the map of pending transactions : " + depositHashkey);
								ExchStatus exchStatus = transxIdToTargetExchMap.remove(depositHashkey);
								LOG.debug("Deleting the transaction Id...");
								transxIdToTargetExchService.delete(new AbstractMap.SimpleEntry<Integer, ExchStatus>(depositHashkey, exchStatus));
								
								saveUpdatedBalance(exchange, exchangeName, myWallet.getLabel(), currency);
								
								// Hitbtc : transfer funds to the trading wallet
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
			LOG.debug("Saving the updated balance...");
			walletService.save(walletEntity);
		} else {
			LOG.error("cannot save balance for " + exchangeName + " -> " + currency.getDisplayName() + " : balance unavailable");
		}
	}

	public void setPendingWithdrawalsMap(ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap) {
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
	}

	public void setTransxIdToTargetExchMap(ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap) {
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
	}

	@Override
	public void run() {
		LOG.info("PendingWithdrawals service has started!");
		while (true) {
			try {
				pollPendingWithdrawals();
				
				LOG.debug("transaction Ids : " + transxIdToTargetExchMap);
				LOG.debug("pending transactions : " + pendingWithdrawalsMap);
				
				LOG.debug("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException e) {
				LOG.error("Unexpected error : " + e);
			}
		}
	}

}

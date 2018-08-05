package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
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
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import arbitrail.libra.exchange.AliasCode;
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
	
	private Wallets wallets;
	private Map<Exchange, String> exchangeMap;
	private List<Currency> currencyList;
	private ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap;
	private ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
	
	@Value("${pending_withdrawals_frequency}")
	private Integer frequency;
	
	public PendingWithdrawalsService() {
		super();
	}
	
	public void init(Wallets wallets, ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap, ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap, List<Currency> currencyList, Map<Exchange, String> exchangeMap) {
		this.exchangeMap = exchangeMap;
		this.currencyList = currencyList;
		this.wallets = wallets;
		this.transxIdToTargetExchMap = transxIdToTargetExchMap;
		this.pendingWithdrawalsMap = pendingWithdrawalsMap;
	}

	private void pollPendingWithdrawals() {
		for (Exchange exchange : exchangeMap.keySet()) {
			String exchangeName = exchange.getExchangeSpecification().getExchangeName();

			// loads all wallets for the exchange : needed to get the depositAddress for XRP
			Map<String, MyWallet> walletsForExchange = wallets.getWalletMap().get(exchangeName);
			// no currencies set up for the exchange
			if (walletsForExchange == null) {
				LOG.warn("Configuration error : Could not find any wallet configuration for account : " + exchangeName);
				LOG.warn("Skipping exchange : " + exchangeName);
				continue;
			}

			try {
				List<FundingRecord> fundingRecords = new ArrayList<>();
				// for Bitfinex there is a request per currency
				List<TradeHistoryParams> tradeHistoryParams = transxService.getTradeHistoryParams(exchange, wallets);
				for (TradeHistoryParams tradeHistoryParam : tradeHistoryParams) {
					fundingRecords.addAll(exchange.getAccountService().getFundingHistory(tradeHistoryParam));
				}
				
				fundingRecords = transxService.retrieveLastTwoDaysOf(fundingRecords);
				LOG.debug("FundingRecords for exchange " + exchangeName + " :");
				LOG.debug("@@ " + fundingRecords);
				
				// we are interested in the pending / cancelled withdrawals from the source exchange and the completed deposits from the target exchange
				for (FundingRecord fundingRecord : fundingRecords) {
					Currency currency = fundingRecord.getCurrency();
					// bitfinex currencies are not generic
					if (ExchangeType.BitFinex.name().equals(exchangeName)) {
						 String aliasCode = AliasCode.getGenericCode(fundingRecord.getCurrency().getCurrencyCode());
						 if (aliasCode == null) {
							 LOG.warn("Skipping currency because it could not find the generic alias code for bitfinex currency : " + fundingRecord.getCurrency().getCurrencyCode());
							 continue;
						 }
						 currency = new Currency(aliasCode);
					}
					
					if (!currencyList.contains(currency)) {
						LOG.warn("Skipping not handled currency : " + currency);
						// skip records for unhandled currencies
						continue;
					}
					
					// compute hashkey for the withdrawal
					Integer transxHashkey = transxService.transxHashkey(currency, fundingRecord.getAmount(), fundingRecord.getAddress());
					LOG.debug("@ Looking for withdrawal transxHashkey : " + transxHashkey + " = (" + currency + ", " + fundingRecord.getAmount() + ", " + fundingRecord.getAddress() + ")");
					if (transxHashkey == null) {
						LOG.error("Unexpected error : transxHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + "$" + currency.getCurrencyCode());
						continue;
					}					
					
					// loads the wallet for the given Exch/Cur : needed to get the depositAddress for XRP
					MyWallet myWallet = walletsForExchange.get(currency.getCurrencyCode());
					// this currency is not set up for the exchange
					if (myWallet == null) {
						LOG.warn("Configuration error : No wallet config found for this account for currency : " + exchangeName + "$" + currency.getCurrencyCode());
						LOG.warn("Skipping currency for exchange : " + exchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					
					// check if the transactions are part of recent transactions handled by Libra
					if (transxIdToTargetExchMap.keySet().contains(transxHashkey)) {
						LOG.debug("@ Withdrawal transxHashkey found in the map of pending transactions : " + transxHashkey);
						// filter pending withdrawals
						if (Type.WITHDRAWAL.equals(fundingRecord.getType())) {
							ExchStatus exchStatus = transxIdToTargetExchMap.get(transxHashkey);
							if (exchStatus == null) {
								LOG.error("Unexpected error : Missing mapping transactionId to destination exchange name");
								LOG.warn("Skipping update of pending withdrawals status from exchange : " + exchangeName + "$" + currency.getCurrencyCode());
								continue;
							}
							// filter trades recorded before the withdrawal
							if (!exchStatus.isAlive(fundingRecord.getDate())) {
								LOG.warn("Filtered a withdraw : " + exchangeName + "$" + currency.getCurrencyCode() + " : " + fundingRecord.getDate() + " < " + exchStatus.getWithdrawalTime());
								continue;
							}
							LOG.warn("Detected a withdraw : " + exchangeName + "$" + currency.getCurrencyCode());
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
									saveUpdatedBalance(exchange, exchangeName, exchangeMap.get(exchange), currency);
								}
							}
						}
					}				
					
					// compute hashkey for the deposit
					String depositAddress = walletService.getDepositAddress(exchange, exchangeName, myWallet, currency);
					Integer depositHashkey = transxService.transxHashkey(currency, fundingRecord.getAmount(), depositAddress);
					LOG.debug("@ Looking for deposit transxHashkey : " + depositHashkey + " = (" + currency + ", " + fundingRecord.getAmount() + ", " + depositAddress + ")");
					if (depositHashkey == null) {
						LOG.error("Unexpected error : depositHashkey is null");
						LOG.warn("Skipping transaction from exchange : " + exchangeName + "$" + currency.getCurrencyCode());
						continue;
					}
					if (transxIdToTargetExchMap.keySet().contains(depositHashkey)) {
						LOG.debug("@ Deposit transxHashkey found in the map of pending transactions : " + depositHashkey);
						// filter completed deposits
						if (Type.DEPOSIT.equals(fundingRecord.getType())) {
							// filter trades recorded before the deposit
							if (!transxIdToTargetExchMap.get(depositHashkey).isAlive(fundingRecord.getDate())) {
								LOG.warn("Filtered a deposit : " + exchangeName + "$"
										+ currency.getCurrencyCode() + " : " + fundingRecord.getDate()
										+ " < " + transxIdToTargetExchMap.get(depositHashkey).getWithdrawalTime());
								continue;
							}
							LOG.warn("Detected a deposit : " + exchangeName + "$" + currency.getCurrencyCode());
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
								
								saveUpdatedBalance(exchange, exchangeName, exchangeMap.get(exchange), currency);
								
								// Hitbtc : transfer funds to the trading wallet
								if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
									LOG.debug("@ Hitbtc -> transferring funds to the trading wallet");
									HitbtcAccountService hitbtcAccountService = (HitbtcAccountService) exchange.getAccountService();
									hitbtcAccountService.transferToTrading(currency, fundingRecord.getAmount());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Unexpected error when retrieving funding history : ", e);
				LOG.warn("Skipping polling withdrawals / deposits status for exchange : " + exchangeName);
				continue;
			}
		}
	}

	private void saveUpdatedBalance(Exchange exchange, String exchangeName, String walletId, Currency currency) throws IOException {
		try {
			BigDecimal newBalance = walletService.getAvailableBalance(exchange, walletId, currency);
			if (newBalance != null) {
				LOG.info("newBalance for " + exchangeName + "$" + currency.getCurrencyCode() + " : " + newBalance);
				WalletEntity walletEntity = new WalletEntity(exchangeName, currency.getCurrencyCode(), newBalance);
				LOG.debug("Saving the updated balance...");
				walletService.save(walletEntity);
			} else {
				LOG.error("cannot save balance for " + exchangeName + "$" + currency.getCurrencyCode() + " : balance unavailable");
			}
		} catch (Exception e) {
			LOG.error("Unexpected exception : " + e.getMessage());
			LOG.error("cannot save balance for " + exchangeName + "$" + currency.getCurrencyCode() + " : balance unavailable");
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
				LOG.fatal("Unexpected exception : ", e);
			}
		}
	}

}

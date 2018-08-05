package arbitrail.libra.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;

import arbitrail.libra.model.Wallets;

public interface TransactionService {

	/**
	 * Generates a key that will identify the transaction
	 * @return hashkey
	 */
	Integer transxHashkey(Currency currency, BigDecimal amountToWithdraw, String depositAddress);
	
	/**
	 * Returns a descending sorted list of FundingRecords gathering the last two days of the history.
	 */
	List<FundingRecord> retrieveLastTwoDaysOf(List<FundingRecord> fundingRecords);
	
	/**
	 * Retrieves the transaction matching the given internalId
	 */
	Optional<FundingRecord> filterByInternalId(List<FundingRecord> fundingRecords, String internalId);
	
	/**
	 * Formats the transaction amount for the given currency
	 */
	BigDecimal roundAmount(BigDecimal amount, Currency currency);	
	
	/**
	 * 
	 */
	public TradeHistoryParams getTradeHistoryParams(Exchange exchange, Currency currency);
	
	/**
	 * 
	 */
	public List<TradeHistoryParams> getTradeHistoryParams(Exchange exchange, Wallets wallets);
}

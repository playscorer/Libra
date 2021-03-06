package arbitrail.libra.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.hitbtc.v2.service.HitbtcFundingHistoryParams;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.springframework.stereotype.Component;

import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.Wallets;

@Component
public class TransactionServiceImpl implements TransactionService {
	
	private final static Logger LOG = Logger.getLogger(TransactionServiceImpl.class);

	@Override
	public Integer transxHashkey(Currency currency, BigDecimal amountToWithdraw, String depositAddress) {
		if (currency == null || amountToWithdraw == null) {
			LOG.error("Unexpected error : The parameters are mandatory to generate the hashkey (currency, amountToWithdraw) : ("
							+ currency + ", " + amountToWithdraw);
			return null;
		}
		Integer hashCode = currency.getCurrencyCode().hashCode() * ((Double) amountToWithdraw.doubleValue()).hashCode();
		if (depositAddress != null) {
			hashCode *= depositAddress.hashCode();
		}
		return hashCode;
	}

	@Override
	public List<FundingRecord> retrieveLastTwoDaysOf(List<FundingRecord> fundingRecords) {
		// sorts the history descending by date
		List<FundingRecord> sortedFundingRecords = fundingRecords.stream().sorted(Comparator.comparing(FundingRecord::getDate).reversed()).collect(Collectors.toList());
		if (sortedFundingRecords.isEmpty()) {
			return sortedFundingRecords;
		}

		// computes the date two days before the max date
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(sortedFundingRecords.get(0).getDate());
		calendar.add(Calendar.DAY_OF_MONTH, -2);
		Date twoLastDays = calendar.getTime();
		
		// filter the list of FundingRecords having date after the last two days
		sortedFundingRecords = sortedFundingRecords.stream().filter(fundingRecord -> fundingRecord.getDate().after(twoLastDays)).collect(Collectors.toList());
		
		return sortedFundingRecords;
	}

	@Override
	public Optional<FundingRecord> filterByInternalId(List<FundingRecord> fundingRecords, String internalId) {
		// sorts the history descending by date and filters by internalId to retrieve the first one
		Optional<FundingRecord> matchingFundingRecord = fundingRecords.stream()
				.sorted(Comparator.comparing(FundingRecord::getDate).reversed())
				.filter(fundingRecord -> fundingRecord.getInternalId().equals(internalId)).findFirst();
		
		return matchingFundingRecord;
	}
	
	@Override
	public BigDecimal roundAmount(BigDecimal amount, Currency currency) {
		int scale = 3; // max 3 decimals is enough, helps avoiding bad rounding issues, and makes transfers easier to read
		if (Currency.XRP.equals(currency))
			scale = 0; // low value ccy
		else if (Currency.XVG.equals(currency))
			scale = 0; // low value ccy
		else if (Currency.NEO.equals(currency))
			scale = 0; // NEO blockchain only accepts integer
		amount = amount.setScale(scale, RoundingMode.FLOOR);
		return amount;
	}
	
	@Override
	public TradeHistoryParams getTradeHistoryParams(Exchange exchange, Currency currency) {
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();
		if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
			HitbtcFundingHistoryParams.Builder builder = new HitbtcFundingHistoryParams.Builder();
			return builder.currency(currency).offset(0).limit(100).build();
		} else {
			return new DefaultTradeHistoryParamCurrency(currency);
		}
	}

	@Override
	public List<TradeHistoryParams> getTradeHistoryParams(Exchange exchange, Wallets wallets) {
		List<TradeHistoryParams> tradeHistoryParams = new ArrayList<>();
		String exchangeName = exchange.getExchangeSpecification().getExchangeName();

		if (ExchangeType.Hitbtc.name().equals(exchangeName)) {
			HitbtcFundingHistoryParams.Builder builder = new HitbtcFundingHistoryParams.Builder();
			tradeHistoryParams.add(builder.offset(0).limit(100).build());
		} else if (ExchangeType.BitFinex.name().equalsIgnoreCase(exchangeName)) {
			for (String currencyName : wallets.getWalletMap().get(exchangeName).keySet()) {
				tradeHistoryParams.add(new DefaultTradeHistoryParamCurrency(new Currency(currencyName)));
			}
		} else {
			tradeHistoryParams.add(new DefaultTradeHistoryParamCurrency());
		}
		
		return tradeHistoryParams;
	}
	
}

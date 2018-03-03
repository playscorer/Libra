package arbitrail.libra.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;

public class TransactionServiceImpl implements TransactionService {
	
	private final static Logger LOG = Logger.getLogger(TransactionServiceImpl.class);

	@Override
	public Integer transxHashkey(Currency currency, BigDecimal amountToWithdraw, String depositAddress) {
		if (currency == null || amountToWithdraw == null) {
			LOG.error("Unexpected error : The parameters are mandatory to generate the hashkey (currency, amountToWithdraw) : ("
							+ currency + ", " + amountToWithdraw);
			return null;
		}
		Integer hashCode = currency.getCurrencyCode().hashCode() * ((Double)amountToWithdraw.doubleValue()).hashCode();
		if (depositAddress != null) {
			hashCode *= depositAddress.hashCode();
			//LOG.info("Computed a hashkey (currency, amountToWithdraw, depositAddress, hashkey) : ("
			//	+ currency + ", " + amountToWithdraw+ ", " + depositAddress+ ", " + hashCode + ")");
		}
		return hashCode;
	}

	@Override
	public List<FundingRecord> retrieveLastTwoDaysOf(List<FundingRecord> fundingRecords) {
		// sorts the history descending by date
		List<FundingRecord> sortedFundingRecords = fundingRecords.stream().sorted(Comparator.comparing(FundingRecord::getDate).reversed()).collect(Collectors.toList());

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
	public Optional<FundingRecord> retrieveExternalId(List<FundingRecord> fundingRecords, String internalId) {
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
			scale = 0;
		else if (Currency.XVG.equals(currency))
			scale = 0;
		amount = amount.setScale(scale, RoundingMode.FLOOR);
		return amount;
	}

}

package arbitrail.libra.exchange;

import java.io.IOException;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.common.dto.BitfinexException;
import org.knowm.xchange.bitfinex.v1.BitfinexAdapters;
import org.knowm.xchange.bitfinex.v1.BitfinexUtils;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexDepositAddressRequest;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexDepositAddressResponse;
import org.knowm.xchange.bitfinex.v1.service.BitfinexAccountService;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.params.TradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;

public class BitfinexAccountCustomService extends BitfinexAccountService {

	public BitfinexAccountCustomService(Exchange exchange) {
		super(exchange);
	}

	@Override
	public String requestDepositAddress(Currency currency, String... arguments) throws IOException {
		BitfinexDepositAddressResponse requestDepositAddressResponse;
		String ccyCode = currency.getCurrencyCode();

		try {
			String type = BitfinexUtils.convertToBitfinexWithdrawalType(ccyCode);

			requestDepositAddressResponse = bitfinex.requestDeposit(apiKey, payloadCreator, signatureCreator,
					new BitfinexDepositAddressRequest(String.valueOf(exchange.getNonceFactory().createValue()), type,
							"exchange", 0));

			if (requestDepositAddressResponse != null) {
				return requestDepositAddressResponse.getAddress();
			} else {
				return null;
			}
		} catch (BitfinexException e) {
			throw handleException(e);
		}
	}

	@Override
	public List<FundingRecord> getFundingHistory(TradeHistoryParams params) throws IOException {
		if (params instanceof TradeHistoryParamCurrency && ((TradeHistoryParamCurrency) params).getCurrency() != null) {
	          String currencyCode = ((TradeHistoryParamCurrency) params).getCurrency().getCurrencyCode();
	          String ccy = AliasCode.getBitfinexCode(currencyCode);
	          if (ccy == null) {
	        	  throw new ExchangeException("Not bitfinex alias found for the following currency : " + currencyCode);	  
	          }
	          return BitfinexAdapters.adaptFundingHistory(getDepositWithdrawalHistory(ccy, null, null, null, null));
	    } else {
	        throw new ExchangeException("Currency must be supplied");
	    }
	}

}

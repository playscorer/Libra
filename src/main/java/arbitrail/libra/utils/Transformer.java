package arbitrail.libra.utils;

import org.apache.log4j.Logger;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Account;
import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.CurrencyAttribute;

public class Transformer {
	
	private final static Logger LOG = Logger.getLogger(Transformer.class);
	
	public static BaseExchange fromAccount(Account account) {
		BaseExchange exchange = null;
		try {
			exchange = ExchangeType.valueOf(ExchangeType.class, account.getName()).getExchangeClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		return exchange;
	}
	
	public static Currency fromCurrency(CurrencyAttribute currency) {
		return Currency.getInstance(currency.getCode());
	}
	
}

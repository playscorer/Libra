package arbitrail.libra.utils;

import org.apache.log4j.Logger;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Account;
import arbitrail.libra.model.AccountToExchange;
import arbitrail.libra.model.MyCurrency;

public class Transformer {
	
	private final static Logger LOG = Logger.getLogger(Transformer.class);
	
	public static BaseExchange fromAccount(Account account) {
		BaseExchange exchange = null;
		try {
			exchange = AccountToExchange.valueOf(AccountToExchange.class, account.getName()).getExchangeClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		return exchange;
	}
	
	public static Currency fromCurrency(MyCurrency currency) {
		return Currency.getInstance(currency.getCode());
	}
	
}

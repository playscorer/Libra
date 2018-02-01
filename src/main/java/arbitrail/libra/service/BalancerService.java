package arbitrail.libra.service;

import java.io.IOException;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Wallets;

public interface BalancerService {
	
	default Exchange createExchange(Exchange exchange, String username, String apiKey, String secretKey) {
		ExchangeSpecification exSpec = exchange.getDefaultExchangeSpecification();
		exSpec.setUserName(username);
		exSpec.setApiKey(apiKey);
		exSpec.setSecretKey(secretKey);
		return ExchangeFactory.INSTANCE.createExchange(exSpec);
	}
	
	List<Currency> listAllHandledCurrencies();
	
	List<Exchange> listAllHandledAccounts();
	
	Wallets loadAllAccountsBalance(List<Exchange> exchangeList, List<Currency> currencyList, boolean init);

	int balanceAccounts(List<Exchange> exchangeList, List<Currency> currencyList, Wallets balances) throws IOException;

}

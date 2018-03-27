package arbitrail.libra.service;

import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Wallets;

public interface InitService {

	default Exchange createExchange(Exchange exchange, String username, String apiKey, String secretKey) {
		ExchangeSpecification exSpec = exchange.getDefaultExchangeSpecification();
		exSpec.setUserName(username);
		exSpec.setApiKey(apiKey);
		exSpec.setSecretKey(secretKey);
		return ExchangeFactory.INSTANCE.createExchange(exSpec);
	}
	
	List<Currency> listAllHandledCurrencies();
	
	List<Exchange> listAllHandledAccounts();
	
	Wallets loadAllWallets(List<Exchange> exchangeList, List<Currency> currencyList, boolean init);
	
}

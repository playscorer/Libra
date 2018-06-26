package arbitrail.libra.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Wallets;

public interface InitService {

	default Exchange createExchange(Exchange exchange, String apiKey, String secretKey) {
		ExchangeSpecification exSpec = exchange.getDefaultExchangeSpecification();
		exSpec.setApiKey(apiKey);
		exSpec.setSecretKey(secretKey);
		return ExchangeFactory.INSTANCE.createExchange(exSpec);
	}
	
	List<Currency> listAllHandledCurrencies();
	
	Map<Exchange, String> listAllHandledAccounts(boolean encryptedKeys);
	
	Wallets loadAllWallets(Set<Exchange> exchangeSet, List<Currency> currencyList, boolean init);
	
}

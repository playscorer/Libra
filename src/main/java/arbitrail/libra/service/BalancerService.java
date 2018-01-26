package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;

import arbitrail.libra.model.Wallets;

public interface BalancerService {
	
	default Exchange createExchange(Exchange exchange, String username, String apiKey, String secretKey) {
		ExchangeSpecification exSpec = exchange.getDefaultExchangeSpecification();
		exSpec.setUserName(username);
		exSpec.setApiKey(apiKey);
		exSpec.setSecretKey(secretKey);
		return ExchangeFactory.INSTANCE.createExchange(exSpec);
	}
	
	Map<Exchange, AccountInfo> connectToExchanges(List<Exchange> exchangeList);
	
	List<Currency> listAllHandledCurrencies();
	
	List<Exchange> listAllHandledAccounts();
	
	Wallets loadAllAccountsBalance(Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList, boolean init);

	Exchange findMostFilledBalance(Map<Exchange, AccountInfo> exchangeMap, Currency currency);
	
	int balanceAccounts(Map<Exchange, AccountInfo> exchangeMap, List<Currency> currencyList, Wallets balances);

	boolean balance(Exchange toExchange, Exchange fromExchange, Currency currency, BigDecimal minResidualBalance,
			String paymentIdforXRP) throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException;

}

package arbitrail.libra.service;

import java.io.IOException;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;

import arbitrail.libra.model.Wallets;

public interface BalancerService {
	
	int balanceAccounts(List<Exchange> exchangeList, List<Currency> currencyList, Wallets balances) throws IOException, InterruptedException;

}

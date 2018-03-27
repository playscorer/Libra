package arbitrail.libra.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import arbitrail.libra.model.Account;
import arbitrail.libra.model.Accounts;
import arbitrail.libra.model.Currencies;
import arbitrail.libra.model.MyCurrency;
import arbitrail.libra.model.MyWallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.utils.Transformer;

@Component
public class InitServiceImpl implements InitService {
	
	private final static Logger LOG = Logger.getLogger(InitServiceImpl.class);
	
	@Autowired
	private FileService fileService;

	@Override
	public List<Currency> listAllHandledCurrencies() {
		List<Currency> currencyList = new ArrayList<>();
		
		try {
			Currencies currencies = fileService.parseCurrencies();
			for (MyCurrency myCurrency : currencies.getCurrency()) {
				currencyList.add(Transformer.fromCurrency(myCurrency));
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return currencyList;
	}
	
	@Override
	public List<Exchange> listAllHandledAccounts() {
		List<Exchange> exchangeList = new ArrayList<>();
		
		try {
			Accounts accounts = fileService.parseAccounts();
			for (Account account : accounts.getAccount()) {
				BaseExchange exchange = Transformer.fromAccount(account);
				exchangeList.add(createExchange(exchange, account.getUsername(), account.getApiKey(), account.getKey()));
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return exchangeList;
	}
	
	@Override
	public Wallets loadAllWallets(List<Exchange> exchangeList, List<Currency> currencyList, boolean init) {
		Wallets wallets = null;
		
		try {
			if (fileService.existsWalletsFile()) {
				wallets = fileService.parseWallets();
				if (init) {
					addNewWallets(wallets, exchangeList, currencyList);
				}
			} else {
				if (!init) {
					LOG.error("The wallets file does not exist or could not be found - please check the file or run Libra in init mode");
					return null;
				}
				Map<String, Map<String, MyWallet>> walletMap = initWallets(exchangeList, currencyList);
				wallets = new Wallets(walletMap);
			}

		} catch (IOException e) {
			LOG.error(e.getMessage(), e.getCause());
		}
		
		return wallets;
	}

	/*
	 * Adds only new wallets from the list of exchanges and currencies to the existing wallets map
	 */
	private void addNewWallets(Wallets wallets, List<Exchange> exchangeList, List<Currency> currencyList) throws IOException {
		Map<String, Map<String, MyWallet>> walletMap = wallets.getWalletMap();
		
		for (Exchange toExchange : exchangeList) {
			String exchangeName = toExchange.getExchangeSpecification().getExchangeName();
			
			Map<String, MyWallet> currencyMap = walletMap.get(exchangeName);
			if (currencyMap == null) {
				LOG.debug("New exchange to be added to wallets file : " + exchangeName);
				currencyMap = new HashMap<>();
				walletMap.put(exchangeName, currencyMap);
			}
			
			for (Currency currency : currencyList) {
				MyWallet wallet = currencyMap.get(currency.getCurrencyCode());
				if (wallet == null) {
					Balance balance = toExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
					if (BigDecimal.ZERO.equals(balance.getAvailable())) {
						LOG.warn("Currency not available : " + currency.getDisplayName() +  " for Exchange : " + exchangeName);
					} else {
						LOG.debug("New currency wallet for : " + exchangeName + " -> " + currency.getDisplayName());
						wallet = new MyWallet(balance.getAvailable());
						currencyMap.put(currency.getCurrencyCode(), wallet);
					}
				}
			}
		}
	}

	/*
	 * Creates a new wallets map from the list of exchanges and currencies
	 */
	private Map<String, Map<String, MyWallet>> initWallets(List<Exchange> exchangeList, List<Currency> currencyList) throws IOException {
		Map<String, Map<String, MyWallet>> walletMap = new HashMap<>();
		
		for (Exchange toExchange : exchangeList) {
			String exchangeName = toExchange.getExchangeSpecification().getExchangeName();
			HashMap<String, MyWallet> currencyMap = new HashMap<>();
			walletMap.put(exchangeName, currencyMap);
			
			for (Currency currency : currencyList) {
				Balance balance = toExchange.getAccountService().getAccountInfo().getWallet().getBalance(currency);
				if (BigDecimal.ZERO.equals(balance.getAvailable())) {
					LOG.warn("Currency not available : " + currency.getDisplayName() +  " for Exchange : " + exchangeName);
				} else {
					MyWallet wallet = new MyWallet(balance.getAvailable());
					currencyMap.put(currency.getCurrencyCode(), wallet);
				}
			}
		}

		return walletMap;
	}

}

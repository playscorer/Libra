package libra;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;

import libra.model.Balances;
import libra.model.MyBalance;
import libra.service.BalancerService;
import libra.service.BalancerServiceImpl;
import libra.utils.Parser;
import libra.utils.Utils;

public class Libra extends Thread {

	private final static Logger LOG = Logger.getLogger(Libra.class);

	private static Balances balances;
	private static BalancerService operations;
	private static List<Currency> currencies;
	private static Map<Exchange, AccountInfo> exchangeMap;
	private Integer frequency;

	public Libra(Properties props) {
		frequency = Integer.valueOf(props.getProperty(Utils.Props.frequency.name()));
	}

	@Override
	public void run() {
		while (true) {
			try {
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
				balances = operations.balanceAccounts(exchangeMap, currencies, balances);
				Parser.saveAccountsBalanceToFile(balances);
			} catch (InterruptedException | IOException e) {
				LOG.error(e);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		Properties props = Utils.loadProperties("src/main/resources/conf.properties");
		LOG.debug("Properties loaded : " + props);

		operations = new BalancerServiceImpl(props);

		currencies = operations.listAllHandledCurrencies();
		LOG.debug("List of loaded currencies : " + currencies);

		List<Exchange> exchanges = operations.listAllHandledAccounts();
		LOG.debug("List of loaded exchanges : " + exchanges);
		
		exchangeMap = operations.connectToExchanges(exchanges);
		LOG.info("Connected to exchanges");

		String initArg = System.getProperty("init");
		boolean init = Boolean.valueOf(initArg);

		if (init) {
			LOG.info("Init mode enabled");
			LOG.debug("Initialization of the accounts balance");
			Map<String, Map<String, MyBalance>> balanceMap = operations.initAccountsBalance(exchangeMap, currencies);
			balances = new Balances(balanceMap);
			try {
				Parser.saveAccountsBalanceToFile(balances);
			} catch (IOException e) {
				LOG.error(e);
			}
		} else {
			LOG.info("Simulation mode : " + Boolean.valueOf(props.getProperty(Utils.Props.simulate.name())));
			LOG.debug("Loading the accounts balance");
			balances = operations.loadAllAccountsBalance();
			new Libra(props).start();
		}

	}

}

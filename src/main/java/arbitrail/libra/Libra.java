package arbitrail.libra;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;

import arbitrail.libra.model.Wallet;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.service.BalancerService;
import arbitrail.libra.service.BalancerServiceImpl;
import arbitrail.libra.service.PendingWithdrawalsService;
import arbitrail.libra.utils.ExchCcy;
import arbitrail.libra.utils.Parser;
import arbitrail.libra.utils.Utils;

public class Libra extends Thread {

	private final static Logger LOG = Logger.getLogger(Libra.class);
	
	private static Wallets wallets;
	private static BalancerService operations;
	private static List<Currency> currencies;
	private static Map<Exchange, AccountInfo> exchangeMap;
	private Integer frequency;

	private static ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap = new ConcurrentHashMap<>();
	private static ConcurrentMap<String, String> pendingTransIdToToExchMap = new ConcurrentHashMap<>();

	public Libra(Properties props) {
		frequency = Integer.valueOf(props.getProperty(Utils.Props.libra_frequency.name()));
	}

	@Override
	public void run() {
		LOG.info("Libra has started!");
		while (true) {
			try {
				wallets = operations.balanceAccounts(exchangeMap, currencies, wallets);
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
				//TODO get the wallets uptodate
				Parser.saveAccountsBalanceToFile(wallets);
			} catch (InterruptedException | IOException e) {
				LOG.error(e);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		Properties props = Utils.loadProperties("src/main/resources/conf.properties");
		LOG.debug("Properties loaded : " + props);

		operations = new BalancerServiceImpl(props, pendingWithdrawalsMap, pendingTransIdToToExchMap);

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
			Map<String, Map<String, Wallet>> balanceMap = operations.initAccountsBalance(exchangeMap, currencies);
			wallets = new Wallets(balanceMap);
			try {
				Parser.saveAccountsBalanceToFile(wallets);
			} catch (IOException e) {
				LOG.error(e);
			}
		} else {
			Boolean simulate = Boolean.valueOf(props.getProperty(Utils.Props.simulate.name()));
			LOG.info("Simulation mode : " + simulate);
			
			if (!simulate) {
				Integer pendingServiceFrequency = Integer.valueOf(props.getProperty(Utils.Props.pending_service_frequency.name()));
				new PendingWithdrawalsService(exchanges, pendingWithdrawalsMap, pendingTransIdToToExchMap, pendingServiceFrequency).start();
			}
			
			LOG.debug("Loading the accounts balance");
			wallets = operations.loadAllAccountsBalance();
			new Libra(props).start();
		}

	}

}

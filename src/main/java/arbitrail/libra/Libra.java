package arbitrail.libra;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.PendingTransxToExchService;
import arbitrail.libra.orm.spring.ContextProvider;
import arbitrail.libra.service.BalancerService;
import arbitrail.libra.service.BalancerServiceImpl;
import arbitrail.libra.service.PendingWithdrawalsService;
import arbitrail.libra.utils.Parser;
import arbitrail.libra.utils.Utils;

public class Libra extends Thread {

	private final static Logger LOG = Logger.getLogger(Libra.class);
	
	private static PendingTransxToExchService pendingTransxToExchService = ContextProvider.getBean(PendingTransxToExchService.class);
	private static PendingTransxService pendingTransxService = ContextProvider.getBean(PendingTransxService.class);
	private static Wallets wallets;
	private static BalancerService operations;
	private static List<Exchange> exchanges;
	private static List<Currency> currencies;
	private Integer frequency;

	private static ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private static ConcurrentMap<String, String> pendingTransIdToToExchMap;

	public Libra(Properties props) {
		frequency = Integer.valueOf(props.getProperty(Utils.Props.libra_frequency.name()));
	}

	@Override
	public void run() {
		int nbOperations;
		LOG.info("Libra has started!");
		while (true) {
			try {
				nbOperations = operations.balanceAccounts(exchanges, currencies, wallets);
				LOG.debug("Number of rebalancing operations : " + nbOperations);
				if (nbOperations > 0) {
					Parser.saveAccountsBalanceToFile(wallets); //TODO must be done once the withdrawal is complete
				}
				LOG.info("Sleeping for (ms) : " + frequency);
				Thread.sleep(frequency);
			} catch (InterruptedException | IOException e) {
				LOG.error(e);
			}
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		Properties props = Utils.loadProperties("src/main/resources/conf.properties");
		LOG.debug("Properties loaded : " + props);

		operations = new BalancerServiceImpl(props, pendingWithdrawalsMap, pendingTransIdToToExchMap);

		currencies = operations.listAllHandledCurrencies();
		LOG.debug("List of loaded currencies : " + currencies);

		exchanges = operations.listAllHandledAccounts();
		LOG.debug("List of loaded exchanges : " + exchanges);
		
		String initArg = System.getProperty("init");
		boolean init = Boolean.valueOf(initArg);

		if (init) {
			LOG.info("Init mode enabled");
			LOG.debug("Initialization of the accounts balance");
			wallets = operations.loadAllAccountsBalance(exchanges, currencies, init);
			try {
				Parser.saveAccountsBalanceToFile(wallets);
			} catch (IOException e) {
				LOG.error(e);
			}
			
		} else {
			// loads spring context
			new ClassPathXmlApplicationContext("classpath:/spring.xml");
			
			Boolean simulate = Boolean.valueOf(props.getProperty(Utils.Props.simulate.name()));
			LOG.info("Simulation mode : " + simulate);
			
			if (!simulate) {
				Integer pendingServiceFrequency = Integer.valueOf(props.getProperty(Utils.Props.pending_service_frequency.name()));
				LOG.info("Loading the pending transactions");
				pendingTransIdToToExchMap = pendingTransxToExchService.listAll();
				LOG.info("Loading the status of the pending transactions");
				pendingWithdrawalsMap = pendingTransxService.listAll();
				new PendingWithdrawalsService(exchanges, pendingWithdrawalsMap, pendingTransIdToToExchMap, pendingServiceFrequency).start();
			}
			
			LOG.debug("Loading the accounts balance");
			wallets = operations.loadAllAccountsBalance(exchanges, currencies, init);
			new Libra(props).start();
		}
	}
}

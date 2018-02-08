package arbitrail.libra;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.orm.spring.ContextProvider;
import arbitrail.libra.service.BalancerService;
import arbitrail.libra.service.BalancerServiceImpl;
import arbitrail.libra.service.PendingWithdrawalsService;
import arbitrail.libra.utils.Parser;
import arbitrail.libra.utils.Utils;

public class Libra extends Thread {

	private final static Logger LOG = Logger.getLogger(Libra.class);
	
	private static TransxIdToTargetExchService transxIdToTargetService = ContextProvider.getBean(TransxIdToTargetExchService.class);
	private static PendingTransxService pendingTransxService = ContextProvider.getBean(PendingTransxService.class);
	private static Wallets wallets;
	private static BalancerService operations;
	private static List<Exchange> exchanges;
	private static List<Currency> currencies;
	private Integer frequency;

	private static ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
	private static ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap;

	public Libra(Properties props) {
		frequency = Integer.valueOf(props.getProperty(Utils.Props.libra_frequency.name()));
	}

	@Override
	public void run() {
		int nbOperations;
		LOG.info("Libra has started!");
		while (true) {
			try {
				LocalDate before = LocalDate.now();
				nbOperations = operations.balanceAccounts(exchanges, currencies, wallets);
				LocalDate after = LocalDate.now();
				LOG.info("Number of rebalancing operations : " + nbOperations + " performed in (s) : " + ChronoUnit.SECONDS.between(before, after));
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
				LOG.info("Loading the transaction Ids");
				transxIdToTargetExchMap = transxIdToTargetService.listAll();
				
				LOG.info("Loading the status of the pending transactions");
				pendingWithdrawalsMap = pendingTransxService.listAll();

				Integer pendingServiceFrequency = Integer.valueOf(props.getProperty(Utils.Props.pending_service_frequency.name()));
				new PendingWithdrawalsService(exchanges, pendingWithdrawalsMap, transxIdToTargetExchMap, pendingServiceFrequency).start();
			} else {
				transxIdToTargetExchMap = new ConcurrentHashMap<>();
				pendingWithdrawalsMap = new ConcurrentHashMap<>();
			}
			
			operations = new BalancerServiceImpl(props, pendingWithdrawalsMap, transxIdToTargetExchMap);
			
			LOG.debug("Loading the accounts balance");
			wallets = operations.loadAllAccountsBalance(exchanges, currencies, init);
			new Libra(props).start();
		}
	}
}

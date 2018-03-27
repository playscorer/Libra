package arbitrail.libra;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.service.BalancerService;
import arbitrail.libra.service.FileService;
import arbitrail.libra.service.InitService;
import arbitrail.libra.service.PendingWithdrawalsService;

@Component
public class Libra {

	private final static Logger LOG = Logger.getLogger(Libra.class);
	
	@Autowired
	private TransxIdToTargetExchService transxIdToTargetService;
	
	@Autowired
	private PendingTransxService pendingTransxService;

	@Value("${simulate}")
	private boolean simulate;
	
	@Autowired
	private InitService initService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private BalancerService balancerService;
	
	@Autowired
	private PendingWithdrawalsService pendingWithdrawalsService;

	@PostConstruct
	public void start() {
		ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
		ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
		
		List<Currency> currencies = initService.listAllHandledCurrencies();
		if (currencies.isEmpty()) {
			LOG.fatal("Currency list is empty - please check the currencies file!");
			LOG.fatal("Libra has stopped!");
			System.exit(-1);
		}
		LOG.info("List of loaded currencies : " + currencies);

		List<Exchange> exchanges = initService.listAllHandledAccounts();
		if (exchanges.isEmpty()) {
			LOG.fatal("Exchange list is empty - please check the accounts file!");
			LOG.fatal("Libra has stopped!");
			System.exit(-1);
		}
		LOG.info("List of loaded exchanges : " + exchanges);
		
		String initArg = System.getProperty("init");
		boolean init = Boolean.valueOf(initArg);

		LOG.info("Initialization of the wallets settings");
		Wallets wallets = initService.loadAllWallets(exchanges, currencies, init);
		if (wallets == null) {
			LOG.fatal("Wallets settings could not be loaded - please check the wallets file!");
			LOG.fatal("Libra has stopped!");
			System.exit(-1);
		}
		LOG.info("Done.");

		if (init) {
			LOG.info("Init mode enabled");
			try {
				fileService.saveWalletsToFile(wallets);
			} catch (IOException e) {
				LOG.error("Error when saving wallets to file", e);
			}
			
		} else {
			LOG.info("Simulation mode : " + simulate);
			
			LOG.info("Loading the transaction Ids");
			transxIdToTargetExchMap = transxIdToTargetService.listAll();
			
			LOG.info("Loading the status of the pending transactions");
			pendingWithdrawalsMap = pendingTransxService.listAll();

			pendingWithdrawalsService.init(wallets, pendingWithdrawalsMap, transxIdToTargetExchMap, exchanges);
			pendingWithdrawalsService.start();
			
			balancerService.init(wallets, pendingWithdrawalsMap, transxIdToTargetExchMap, currencies, exchanges);
			balancerService.start();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		// loads spring context
		new ClassPathXmlApplicationContext("classpath:/spring.xml");
	}
}

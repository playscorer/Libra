package arbitrail.libra;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import arbitrail.libra.model.CurrencyAttribute;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.orm.service.PendingTransxService;
import arbitrail.libra.orm.service.TransxIdToTargetExchService;
import arbitrail.libra.service.BalancerService;
import arbitrail.libra.service.FileService;
import arbitrail.libra.service.InitService;
import arbitrail.libra.service.LibraPoolService;
import arbitrail.libra.service.PendingWithdrawalsService;

@Component
public class Libra {

	private final static Logger LOG = Logger.getLogger(Libra.class);
	
	@Autowired
	private LibraPoolService libraPoolService;
	
	@Autowired
	private TransxIdToTargetExchService transxIdToTargetService;
	
	@Autowired
	private PendingTransxService pendingTransxService;

	@Value("${simulate}")
	private boolean simulate;
	
	@Value("${encrypted}")
	private boolean encrypted;
	
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
		ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap;
		ConcurrentMap<Integer, ExchStatus> transxIdToTargetExchMap;
		Map<String, CurrencyAttribute> currencyAttributesMap = new HashMap<>();
		
		List<Currency> currencies = initService.listAllHandledCurrencies(currencyAttributesMap);
		if (currencies.isEmpty()) {
			LOG.fatal("Currency list is empty - please check the currencies file!");
			LOG.fatal("Libra has stopped!");
			System.exit(-1);
		}
		LOG.info("List of loaded currencies : " + currencies);

		Map<Exchange, String> exchangesMap = initService.listAllHandledAccounts(encrypted);
		if (exchangesMap.isEmpty()) {
			LOG.fatal("Exchange list is empty - please check the accounts file!");
			LOG.fatal("Libra has stopped!");
			System.exit(-1);
		}
		LOG.info("List of loaded exchanges : " + exchangesMap.keySet());
		
		String initArg = System.getProperty("init");
		boolean init = Boolean.valueOf(initArg);

		LOG.info("Initialization of the wallets settings");
		Wallets wallets = initService.loadAllWallets(exchangesMap.keySet(), currencies, init);
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
			
			transxIdToTargetExchMap = transxIdToTargetService.listAll();
			LOG.debug("Loaded transaction Ids : " + transxIdToTargetExchMap);
			
			pendingWithdrawalsMap = pendingTransxService.listAll();
			LOG.debug("Loaded pending transactions : " + pendingWithdrawalsMap);
			
			pendingWithdrawalsService.init(wallets, pendingWithdrawalsMap, transxIdToTargetExchMap, currencies, exchangesMap);
			libraPoolService.startService(pendingWithdrawalsService);
			
			balancerService.init(wallets, pendingWithdrawalsMap, transxIdToTargetExchMap, currencies, currencyAttributesMap, exchangesMap);
			libraPoolService.startService(balancerService);
			
			libraPoolService.shutServices();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		// loads spring context
		new ClassPathXmlApplicationContext("classpath:/spring.xml");
	}
}

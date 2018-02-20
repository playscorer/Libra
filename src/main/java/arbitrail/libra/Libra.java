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
import arbitrail.libra.service.InitService;
import arbitrail.libra.service.PendingWithdrawalsService;
import arbitrail.libra.utils.Parser;

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
	private BalancerService balancerService;
	
	@Autowired
	private PendingWithdrawalsService pendingWithdrawalsService;

	@PostConstruct
	public void start() {
		Wallets wallets;
		ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap;
		ConcurrentMap<String, ExchStatus> transxIdToTargetExchMap;
		
		List<Currency> currencies = initService.listAllHandledCurrencies();
		LOG.debug("List of loaded currencies : " + currencies);

		List<Exchange> exchanges = initService.listAllHandledAccounts();
		LOG.debug("List of loaded exchanges : " + exchanges);
		
		String initArg = System.getProperty("init");
		boolean init = Boolean.valueOf(initArg);

		if (init) {
			LOG.info("Init mode enabled");
			LOG.debug("Initialization of the accounts balance");
			wallets = initService.loadAllAccountsBalance(exchanges, currencies, init);
			try {
				Parser.saveAccountsBalanceToFile(wallets);
			} catch (IOException e) {
				LOG.error(e);
			}
			
		} else {
			LOG.info("Simulation mode : " + simulate);
			
			LOG.debug("Loading the accounts balance");
			wallets = initService.loadAllAccountsBalance(exchanges, currencies, init);
			
			LOG.info("Loading the transaction Ids");
			transxIdToTargetExchMap = transxIdToTargetService.listAll();
			
			LOG.info("Loading the status of the pending transactions");
			pendingWithdrawalsMap = pendingTransxService.listAll();

			pendingWithdrawalsService.init(pendingWithdrawalsMap, transxIdToTargetExchMap, exchanges);
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

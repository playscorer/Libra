package arbitrail.libra.orm.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.MyWallet;
import arbitrail.libra.orm.dao.WalletDao;
import arbitrail.libra.orm.model.WalletEntity;

@Service
public class WalletService {
	
	private final static Logger LOG = Logger.getLogger(WalletService.class);
	
	@Autowired
	private WalletDao walletDao;

	@Transactional
	public void save(WalletEntity wallet) {
		walletDao.saveOrUpdate(wallet);
	}
	
	@Transactional(readOnly = true)
	public BigDecimal getLastBalancedAmount(String exchange, String currency) {
		WalletEntity entity = walletDao.find(exchange, currency);
		return entity == null ? BigDecimal.ZERO : entity.getLastBalancedAmount() == null ? BigDecimal.ZERO : entity.getLastBalancedAmount();
	}	
	
	@Transactional(readOnly = true)
	public List<WalletEntity> listAll() {
		return walletDao.findAll();

	}
	
	/**
	 * Handles single and multiple wallets per currency and exclude the zero balance
	 * in order to avoid synchronization bugs with the exchange.
	 */
	public BigDecimal getAvailableBalance(Exchange exchange, String walletId, Currency currency) throws IOException {
		// find the wallet depending if it is an exchange with multiple wallets per currency
		Wallet wallet;
		if (walletId == null) {
			wallet = exchange.getAccountService().getAccountInfo().getWallet();
		} else {
			wallet = exchange.getAccountService().getAccountInfo().getWallet(walletId);
		}
		// return the available balance if it is not zero
		Balance balance = wallet.getBalance(currency);
		return balance.getAvailable();
	}
	
	/**
	 * Returns the deposit address for the specified wallet : for Bittrex with XRP it looks into the file config. 
	 */
	public String getDepositAddress(Exchange toExchange, String exchangeName, MyWallet toWallet, Currency currency) throws IOException {
		String depositAddress;
		if (ExchangeType.Bittrex.name().equals(exchangeName) && Currency.XRP.equals(currency)) {
			depositAddress = toWallet.getAddress();
			if (depositAddress == null) {
				LOG.warn("Configuration error : Deposit address required for Bittrex with XRP");
			}
		} else {
			depositAddress = toExchange.getAccountService().requestDepositAddress(currency);
		}
		// specific case for XRP and Bitstamp
/*		if (Currency.XRP.equals(fundingRecord.getCurrency()) && ExchangeType.Bitstamp.name().equals(exchangeName)) {
			depositAddress = ((BitstampAccountService) exchange.getAccountService()).getRippleDepositAddress().getAddress();
		}*/
		return depositAddress;
	}
}

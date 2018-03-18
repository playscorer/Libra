package arbitrail.libra.orm.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.orm.dao.WalletDao;
import arbitrail.libra.orm.model.WalletEntity;

@Service
public class WalletService {

	@Autowired
	private WalletDao walletDao;

	@Transactional
	public void save(WalletEntity wallet) {
		walletDao.persist(wallet);
	}
	
	@Transactional
	public void saveAll(Collection<WalletEntity> walletCollection) {
		for (WalletEntity wallet : walletCollection) {
			walletDao.persist(wallet);
		}
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
	
	/** Handles exchanges with single or multiple wallets per currency */
	public Wallet getWallet(Exchange exchange, arbitrail.libra.model.Wallet libraWallet) throws IOException	{
		if (libraWallet.getLabel() == null) {
			return exchange.getAccountService().getAccountInfo().getWallet();
		}
		return exchange.getAccountService().getAccountInfo().getWallet(libraWallet.getLabel());
	}
	
	/** Handles synchronization bugs with exchange (exclude zero update) */
	public Balance getBalance(Wallet wallet, Currency currency) throws IOException	{
		Balance balance = wallet.getBalance(currency);
		return BigDecimal.ZERO.equals(balance.getAvailable()) ? null : balance;
	}	
}

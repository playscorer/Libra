package arbitrail.libra.orm.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitstamp.dto.account.BitstampBalance.Balance;
import org.knowm.xchange.currency.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.model.Wallet;
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
	
	// handles exchanges with single or multiple wallets per currency
	public org.knowm.xchange.dto.account.Wallet getWallet(Exchange exchange, Wallet wallet) throws IOException	{
		org.knowm.xchange.dto.account.Wallet exchgWallet = null;
		Map<String, org.knowm.xchange.dto.account.Wallet> walletsTmp = exchange.getAccountService().getAccountInfo().getWallets();
		if (wallet.getLabel() != null)
		{
			for (Map.Entry<String, org.knowm.xchange.dto.account.Wallet> entry : walletsTmp.entrySet())
			{
				if (wallet.getLabel().equals(entry.getKey()))
				{
					exchgWallet = entry.getValue();
					break;
				}
			}
		}
		else
			exchgWallet = exchange.getAccountService().getAccountInfo().getWallet();
		return exchgWallet;
	}
	
	// handles synchronization bugs with exchange (exclude zero update)
	public org.knowm.xchange.dto.account.Balance getBalance(org.knowm.xchange.dto.account.Wallet wallet, Currency currency) throws IOException	{
		org.knowm.xchange.dto.account.Balance balance = wallet.getBalance(currency);
		if (balance.getAvailable().doubleValue() == 0.0)
			return null;
		return balance;
	}
	
}

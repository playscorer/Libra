package arbitrail.libra.orm.service;

import java.util.Collection;
import java.util.List;

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
	public void saveAll(Collection<WalletEntity> walletCollection) {
		for (WalletEntity wallet : walletCollection) {
			walletDao.persist(wallet);
		}
	}

	@Transactional(readOnly = true)
	public List<WalletEntity> listAll() {
		return walletDao.findAll();

	}
	
}

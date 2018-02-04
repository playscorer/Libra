package arbitrail.libra.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import arbitrail.libra.orm.model.WalletEntity;

@Component
public class WalletDao {

	@PersistenceContext
	private EntityManager em;

	public void persist(WalletEntity wallet) {
		em.persist(wallet);
	}

	@SuppressWarnings("unchecked")
	public List<WalletEntity> findAll() {
		return em.createQuery("SELECT w FROM WalletEntity w").getResultList();
	}
	
}

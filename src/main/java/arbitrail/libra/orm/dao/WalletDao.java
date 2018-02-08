package arbitrail.libra.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Component;

import arbitrail.libra.orm.model.WalletEntity;

@Component
public class WalletDao {

	@PersistenceContext
	private EntityManager em;

	public void persist(WalletEntity wallet) {
		em.persist(wallet);
	}
	
	public WalletEntity find(String exchange, String currency) {
		TypedQuery<WalletEntity> query = em.createQuery("SELECT w FROM WalletEntity w WHERE w.exchange = ?1 AND w.currency = ?2", WalletEntity.class);
		query.setParameter(1, exchange);
		query.setParameter(2, currency);
		return query.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public List<WalletEntity> findAll() {
		return em.createQuery("SELECT w FROM WalletEntity w").getResultList();
	}
	
}

package arbitrail.libra.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import arbitrail.libra.orm.model.PendingTransxToExchEntity;

@Component
public class PendingTransxToExchDao {
	
	@PersistenceContext
	private EntityManager em;

	public void persist(PendingTransxToExchEntity pendingTransxToExch) {
		em.persist(pendingTransxToExch);
	}

	@SuppressWarnings("unchecked")
	public List<PendingTransxToExchEntity> findAll() {
		return em.createQuery("SELECT pte FROM PendingTransxToExchEntity pte").getResultList();
	}
	
}

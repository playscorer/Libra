package arbitrail.libra.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import arbitrail.libra.orm.model.TransxIdToTargetExchEntity;

@Component
public class TransxIdToTargetExchDao {
	
	@PersistenceContext
	private EntityManager em;

	public void persist(TransxIdToTargetExchEntity transxIdToTargetExch) {
		em.persist(transxIdToTargetExch);
	}

	@SuppressWarnings("unchecked")
	public List<TransxIdToTargetExchEntity> findAll() {
		return em.createQuery("SELECT pte FROM TransxIdToTargetExchEntity pte").getResultList();
	}
	
}

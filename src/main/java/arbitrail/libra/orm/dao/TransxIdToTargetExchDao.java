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

	public void save(TransxIdToTargetExchEntity transxIdToTargetExch) {
		em.persist(transxIdToTargetExch);
	}

	public void delete(TransxIdToTargetExchEntity transxIdToTargetExch) {
		TransxIdToTargetExchEntity managed = em.merge(transxIdToTargetExch);
		em.remove(managed);
	}

	@SuppressWarnings("unchecked")
	public List<TransxIdToTargetExchEntity> findAll() {
		return em.createQuery("SELECT pte FROM TransxIdToTargetExchEntity pte").getResultList();
	}
	
}

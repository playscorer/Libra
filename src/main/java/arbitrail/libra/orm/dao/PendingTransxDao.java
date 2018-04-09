package arbitrail.libra.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import arbitrail.libra.orm.model.PendingTransxEntity;

@Component
public class PendingTransxDao {

	@PersistenceContext
	private EntityManager em;

	public void saveOrUpdate(PendingTransxEntity pendingTransx) {
		em.merge(pendingTransx);
	}

	@SuppressWarnings("unchecked")
	public List<PendingTransxEntity> findAll() {
		return em.createQuery("SELECT pt FROM PendingTransxEntity pt").getResultList();
	}
	
}

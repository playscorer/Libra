package arbitrail.libra.orm.service;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.orm.dao.PendingTransxDao;
import arbitrail.libra.orm.model.PendingTransxEntity;

@Service
public class PendingTransxService {
	
	@Autowired
	private PendingTransxDao pendingTransxDao;

	@Transactional
	public void save(PendingTransxEntity pendingTransx) {
		pendingTransxDao.persist(pendingTransx);
	}
	
	@Transactional
	public void saveAll(Collection<PendingTransxEntity> pendingTransxCollection) {
		for (PendingTransxEntity pendingTransx : pendingTransxCollection) {
			pendingTransxDao.persist(pendingTransx);
		}
	}

	@Transactional(readOnly = true)
	public List<PendingTransxEntity> listAll() {
		return pendingTransxDao.findAll();

	}
	
}

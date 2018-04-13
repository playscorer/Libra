package arbitrail.libra.orm.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.model.ExchCcy;
import arbitrail.libra.orm.dao.PendingTransxDao;
import arbitrail.libra.orm.model.PendingTransxEntity;

@Service
public class PendingTransxService {
	
	@Autowired
	private PendingTransxDao pendingTransxDao;

	@Transactional
	public void save(ExchCcy exchCcy) {
		pendingTransxDao.saveOrUpdate(new PendingTransxEntity(exchCcy.getExchangeName(), exchCcy.getCurrencyCode()));
	}

	@Transactional
	public void delete(ExchCcy exchCcy) {
		pendingTransxDao.delete(new PendingTransxEntity(exchCcy.getExchangeName(), exchCcy.getCurrencyCode()));
	}

	@Transactional(readOnly = true)
	public ConcurrentMap<ExchCcy, Object> listAll() {
		List<PendingTransxEntity> entityList = pendingTransxDao.findAll();
		
		ConcurrentMap<ExchCcy, Object> pendingWithdrawalsMap = new ConcurrentHashMap<>();
		for (PendingTransxEntity entity : entityList) {
			ExchCcy exchCcy = new ExchCcy(entity.getExchangeName(), entity.getCurrencyCode());
			pendingWithdrawalsMap.put(exchCcy, new Object());
		}
		
		return pendingWithdrawalsMap; 
	}
	
}

package arbitrail.libra.orm.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	public void saveAll(Map<ExchCcy, Boolean> pendingWithdrawalsMap) {
		for (Entry<ExchCcy, Boolean> entry : pendingWithdrawalsMap.entrySet()) {
			ExchCcy exchCcy = entry.getKey();
			Boolean status = entry.getValue();
			pendingTransxDao.persist(new PendingTransxEntity(exchCcy.getExchangeName(), exchCcy.getCurrencyCode(), status));
		}
	}

	@Transactional(readOnly = true)
	public ConcurrentMap<ExchCcy, Boolean> listAll() {
		List<PendingTransxEntity> entityList = pendingTransxDao.findAll();
		
		ConcurrentMap<ExchCcy, Boolean> pendingWithdrawalsMap = new ConcurrentHashMap<>();
		for (PendingTransxEntity entity : entityList) {
			ExchCcy exchCcy = new ExchCcy(entity.getExchange(), entity.getCurrency());
			pendingWithdrawalsMap.put(exchCcy, entity.isStatus());
		}
		
		return pendingWithdrawalsMap; 
	}
	
}

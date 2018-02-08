package arbitrail.libra.orm.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.orm.dao.PendingTransxToExchDao;
import arbitrail.libra.orm.model.PendingTransxToExchEntity;

@Service
public class PendingTransxToExchService {

	@Autowired
	private PendingTransxToExchDao pendingTransxToExchDao;
	
	@Transactional
	public void saveAll(Map<String, String> pendingTransIdToToExchMap) {
		for (Entry<String, String> entry : pendingTransIdToToExchMap.entrySet()) {
			String transxId = entry.getKey();
			String exchange = entry.getValue();
			pendingTransxToExchDao.persist(new PendingTransxToExchEntity(null, transxId, exchange));
		}
	}

	@Transactional(readOnly = true)
	public ConcurrentMap<String, String> listAll() {
		List<PendingTransxToExchEntity> entityList = pendingTransxToExchDao.findAll();
		
		ConcurrentMap<String, String> pendingTransIdToToExchMap = new ConcurrentHashMap<>();
		for (PendingTransxToExchEntity entity : entityList) {
			pendingTransIdToToExchMap.put(entity.getTransxId(), entity.getExchange());
		}
		
		return pendingTransIdToToExchMap; 
	}
}

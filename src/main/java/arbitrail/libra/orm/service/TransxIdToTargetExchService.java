package arbitrail.libra.orm.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.model.ExchStatus;
import arbitrail.libra.orm.dao.TransxIdToTargetExchDao;
import arbitrail.libra.orm.model.TransxIdToTargetExchEntity;

@Service
public class TransxIdToTargetExchService {

	@Autowired
	private TransxIdToTargetExchDao transxIdToTargetExchDao;
	
	@Transactional
	public void saveAll(Map<String, ExchStatus> transxIdToTargetExchMqp) {
		for (Entry<String, ExchStatus> entry : transxIdToTargetExchMqp.entrySet()) {
			String transxId = entry.getKey();
			ExchStatus exchStatus = entry.getValue();
			transxIdToTargetExchDao.persist(new TransxIdToTargetExchEntity(transxId, exchStatus.getExchangeName(), exchStatus.isWithdrawalComplete()));
		}
	}

	@Transactional(readOnly = true)
	public ConcurrentMap<String, ExchStatus> listAll() {
		List<TransxIdToTargetExchEntity> entityList = transxIdToTargetExchDao.findAll();
		
		ConcurrentMap<String, ExchStatus> pendingTransIdToToExchMap = new ConcurrentHashMap<>();
		for (TransxIdToTargetExchEntity entity : entityList) {
			pendingTransIdToToExchMap.put(entity.getTransxId(), new ExchStatus(entity.getExchange(), entity.isWithdrawalComplete()));
		}
		
		return pendingTransIdToToExchMap; 
	}
}

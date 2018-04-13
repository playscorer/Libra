package arbitrail.libra.orm.service;

import java.util.List;
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
	public void save(Entry<Integer, ExchStatus> entry) {
		Integer transxId = entry.getKey();
		ExchStatus exchStatus = entry.getValue();
		transxIdToTargetExchDao.save(new TransxIdToTargetExchEntity(transxId, exchStatus.getExchangeName(), exchStatus.isWithdrawalComplete(), exchStatus.getWithdrawalTime()));
	}

	@Transactional
	public void delete(Entry<Integer, ExchStatus> entry) {
		Integer transxId = entry.getKey();
		ExchStatus exchStatus = entry.getValue();
		transxIdToTargetExchDao.delete(new TransxIdToTargetExchEntity(transxId, exchStatus.getExchangeName(), exchStatus.isWithdrawalComplete(), exchStatus.getWithdrawalTime()));
	}

	@Transactional(readOnly = true)
	public ConcurrentMap<Integer, ExchStatus> listAll() {
		List<TransxIdToTargetExchEntity> entityList = transxIdToTargetExchDao.findAll();
		
		ConcurrentMap<Integer, ExchStatus> pendingTransIdToToExchMap = new ConcurrentHashMap<>();
		for (TransxIdToTargetExchEntity entity : entityList) {
			pendingTransIdToToExchMap.put(entity.getTransxId(), new ExchStatus(entity.getExchangeName(), entity.isWithdrawalComplete(), entity.getWithdrawalTime()));
		}
		
		return pendingTransIdToToExchMap; 
	}
}

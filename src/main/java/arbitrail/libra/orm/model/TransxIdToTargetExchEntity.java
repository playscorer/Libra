package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TransxIdToTargetExchEntity {

	@Id private String transxId;
	private String exchange;
	private boolean withdrawalComplete;

	public TransxIdToTargetExchEntity() {
	}

	public TransxIdToTargetExchEntity(String transxId, String exchange, boolean withdrawalComplete) {
		this.transxId = transxId;
		this.exchange = exchange;
		this.withdrawalComplete = withdrawalComplete;
	}

	public String getTransxId() {
		return transxId;
	}

	public String getExchange() {
		return exchange;
	}

	public boolean isWithdrawalComplete() {
		return withdrawalComplete;
	}

}

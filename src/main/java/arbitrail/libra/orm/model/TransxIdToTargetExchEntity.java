package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TransxIdToTargetExchEntity {

	@Id private String transxId;
	private String exchangeName;
	private boolean withdrawalComplete;

	public TransxIdToTargetExchEntity() {
	}

	public TransxIdToTargetExchEntity(String transxId, String exchangeName, boolean withdrawalComplete) {
		super();
		this.transxId = transxId;
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
	}

	public String getTransxId() {
		return transxId;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public boolean isWithdrawalComplete() {
		return withdrawalComplete;
	}

	@Override
	public String toString() {
		return "TransxIdToTargetExchEntity [transxId=" + transxId + ", exchangeName=" + exchangeName
				+ ", withdrawalComplete=" + withdrawalComplete + "]";
	}

}

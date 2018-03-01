package arbitrail.libra.orm.model;

import java.time.LocalTime;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TransxIdToTargetExchEntity {

	@Id private String transxId;
	private String exchangeName;
	private boolean withdrawalComplete;
	private LocalTime withdrawalTime;

	public TransxIdToTargetExchEntity() {
	}

	public TransxIdToTargetExchEntity(String transxId, String exchangeName, boolean withdrawalComplete, LocalTime withdrawalTime) {
		super();
		this.transxId = transxId;
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
		this.withdrawalTime = withdrawalTime;
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
	
	public LocalTime withdrawTime() {
		return this.withdrawalTime;
	}

	@Override
	public String toString() {
		return "TransxIdToTargetExchEntity [transxId=" + transxId + ", exchangeName=" + exchangeName
				+ ", withdrawalComplete=" + withdrawalComplete + "]";
	}

}

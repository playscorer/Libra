package arbitrail.libra.orm.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TransxIdToTargetExchEntity {

	@Id private Integer transxId;
	private String exchangeName;
	private boolean withdrawalComplete;
	private Date withdrawalTime;

	public TransxIdToTargetExchEntity() {
	}

	public TransxIdToTargetExchEntity(Integer transxId, String exchangeName, boolean withdrawalComplete, Date withdrawalTime) {
		super();
		this.transxId = transxId;
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
		this.withdrawalTime = withdrawalTime;
	}

	public Integer getTransxId() {
		return transxId;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public boolean isWithdrawalComplete() {
		return withdrawalComplete;
	}
	
	public Date getWithdrawalTime() {
		return withdrawalTime;
	}

	@Override
	public String toString() {
		return "TransxIdToTargetExchEntity [transxId=" + transxId + ", exchangeName=" + exchangeName
				+ ", withdrawalComplete=" + withdrawalComplete + ", withdrawalTime=" + withdrawalTime + "]";
	}

}

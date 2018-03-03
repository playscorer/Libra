package arbitrail.libra.model;

import java.util.Date;

public class ExchStatus {

	private String exchangeName;
	private boolean withdrawalComplete;
	private Date withdrawalTime;

	public ExchStatus(String exchangeName, boolean withdrawalComplete, Date withdrawalTime) {
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
		this.withdrawalTime = withdrawalTime;
	}

	public String getExchangeName() {
		return exchangeName;
	}
	
	public Date withdrawTime() {
		return this.withdrawalTime;
	}
	
	public boolean isLive(Date curTime) {
		return curTime.after(this.withdrawalTime);
	}

	public boolean isWithdrawalComplete() {
		return withdrawalComplete;
	}

	public void setWithdrawalComplete(boolean withdrawalComplete) {
		this.withdrawalComplete = withdrawalComplete;
	}

	@Override
	public String toString() {
		return "ExchStatus [exchangeName=" + exchangeName + ", withdrawalComplete=" + withdrawalComplete + "]";
	}

}

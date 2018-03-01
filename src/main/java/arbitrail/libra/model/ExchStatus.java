package arbitrail.libra.model;

import java.time.LocalTime;
import java.util.Date;

public class ExchStatus {

	private String exchangeName;
	private boolean withdrawalComplete;
	private LocalTime withdrawalTime;

	public ExchStatus(String exchangeName, boolean withdrawalComplete, LocalTime withdrawalTime) {
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
		this.withdrawalTime = withdrawalTime;
	}

	public String getExchangeName() {
		return exchangeName;
	}
	
	public LocalTime withdrawTime() {
		return this.withdrawalTime;
	}
	
	public boolean isLive(LocalTime curTime) {
		return curTime.isAfter(this.withdrawalTime);
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

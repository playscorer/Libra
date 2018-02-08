package arbitrail.libra.model;

public class ExchStatus {

	private String exchangeName;
	private boolean withdrawalComplete;

	public ExchStatus(String exchangeName, boolean withdrawalComplete) {
		this.exchangeName = exchangeName;
		this.withdrawalComplete = withdrawalComplete;
	}

	public String getExchangeName() {
		return exchangeName;
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

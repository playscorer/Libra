package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Wallet {
	
	@JacksonXmlProperty(localName = "initialBalance")
	private BigDecimal initialBalance;
	
	@JacksonXmlProperty(localName = "lastBalancedAmount")
	private BigDecimal lastBalancedAmount;
	
	@JacksonXmlProperty(localName = "minResidualBalance")
	private BigDecimal minResidualBalance;

	@JacksonXmlProperty(localName = "tag")
	private String tag;

	@JacksonXmlProperty(localName = "depositFee")
	private BigDecimal depositFee;
	
	@JacksonXmlProperty(localName = "withdrawalFee")
	private BigDecimal withdrawalFee;
	
	public Wallet() {
		super();
	}

	public Wallet(BigDecimal initialBalance, BigDecimal lastBalancedAmount) {
		this(initialBalance, lastBalancedAmount, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public Wallet(BigDecimal initialBalance, BigDecimal lastBalancedAmount, BigDecimal minResidualBalance,
			String tag, BigDecimal depositFee, BigDecimal withdrawalFee) {
		super();
		this.initialBalance = initialBalance;
		this.lastBalancedAmount = lastBalancedAmount;
		this.minResidualBalance = minResidualBalance;
		this.tag = tag;
		this.depositFee = depositFee;
		this.withdrawalFee = withdrawalFee;
	}

	public BigDecimal getInitialBalance() {
		return initialBalance;
	}

	public void setInitialBalance(BigDecimal initialBalance) {
		this.initialBalance = initialBalance;
	}

	public BigDecimal getLastBalancedAmount() {
		return lastBalancedAmount;
	}

	public void setLastBalancedAmount(BigDecimal lastBalancedAmount) {
		this.lastBalancedAmount = lastBalancedAmount;
	}

	public BigDecimal getMinResidualBalance() {
		return minResidualBalance;
	}

	public void setMinResidualBalance(BigDecimal minResidualBalance) {
		this.minResidualBalance = minResidualBalance;
	}

	public BigDecimal maxBalance() {
		return initialBalance.max(lastBalancedAmount);
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public BigDecimal getDepositFee() {
		return depositFee;
	}

	public void setDepositFee(BigDecimal depositFee) {
		this.depositFee = depositFee;
	}

	public BigDecimal getWithdrawalFee() {
		return withdrawalFee;
	}

	public void setWithdrawalFee(BigDecimal withdrawalFee) {
		this.withdrawalFee = withdrawalFee;
	}

	@Override
	public String toString() {
		return "Wallet [initialBalance=" + initialBalance + ", lastBalancedAmount=" + lastBalancedAmount
				+ ", minResidualBalance=" + minResidualBalance + ", tag=" + tag
				+ ", depositFee=" + depositFee + ", withdrawalFee=" + withdrawalFee + "]";
	}

}

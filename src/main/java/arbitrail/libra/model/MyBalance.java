package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class MyBalance {
	
	@JacksonXmlProperty(localName = "initialBalance")
	private BigDecimal initialBalance;
	
	@JacksonXmlProperty(localName = "lastBalancedAmount")
	private BigDecimal lastBalancedAmount;
	
	@JacksonXmlProperty(localName = "minResidualBalance")
	private BigDecimal minResidualBalance;

	public MyBalance() {
		super();
	}

	public MyBalance(BigDecimal initialBalance, BigDecimal lastBalancedAmount, BigDecimal minResidualBalance) {
		super();
		this.initialBalance = initialBalance;
		this.lastBalancedAmount = lastBalancedAmount;
		this.minResidualBalance = minResidualBalance;
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

	@Override
	public String toString() {
		return "MyBalance [initialBalance=" + initialBalance + ", lastBalancedAmount=" + lastBalancedAmount
				+ ", minResidualBalance=" + minResidualBalance + "]";
	}

}

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

	@JacksonXmlProperty(localName = "paymentIdForXRP")
	private String paymentIdForXRP;
	
	public Wallet() {
		super();
	}

	public Wallet(BigDecimal initialBalance, BigDecimal lastBalancedAmount, BigDecimal minResidualBalance) {
		this(initialBalance, lastBalancedAmount, minResidualBalance, null);
	}

	public Wallet(BigDecimal initialBalance, BigDecimal lastBalancedAmount, BigDecimal minResidualBalance, String paymentIdForXRP) {
		super();
		this.initialBalance = initialBalance;
		this.lastBalancedAmount = lastBalancedAmount;
		this.minResidualBalance = minResidualBalance;
		this.paymentIdForXRP = paymentIdForXRP;
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

	public String getPaymentIdForXRP() {
		return paymentIdForXRP;
	}

	public void setPaymentIdForXRP(String paymentIdForXRP) {
		this.paymentIdForXRP = paymentIdForXRP;
	}

	@Override
	public String toString() {
		return "Wallet [initialBalance=" + initialBalance + ", lastBalancedAmount=" + lastBalancedAmount
				+ ", minResidualBalance=" + minResidualBalance + ", paymentIdForXRP=" + paymentIdForXRP + "]";
	}

}

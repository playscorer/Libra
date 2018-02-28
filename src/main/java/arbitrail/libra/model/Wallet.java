package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Wallet {

	@JacksonXmlProperty(localName = "initialBalance")
	private BigDecimal initialBalance;

	@JacksonXmlProperty(localName = "minResidualBalance")
	private BigDecimal minResidualBalance;
	
	@JacksonXmlProperty(localName = "minWithdrawalAmount")
	private BigDecimal minWithdrawalAmount;

	@JacksonXmlProperty(localName = "tag")
	private String tag;

	@JacksonXmlProperty(localName = "depositFee")
	private BigDecimal depositFee;

	@JacksonXmlProperty(localName = "withdrawalFee")
	private BigDecimal withdrawalFee;
	
	@JacksonXmlProperty(localName = "label")
	private String label;

	public Wallet() {
		super();
	}

	public Wallet(BigDecimal initialBalance) {
		this(initialBalance, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public Wallet(BigDecimal initialBalance, BigDecimal minResidualBalance, String tag, BigDecimal depositFee, BigDecimal withdrawalFee) {
		super();
		this.initialBalance = initialBalance;
		this.minResidualBalance = minResidualBalance;
		this.tag = tag;
		this.depositFee = depositFee;
		this.withdrawalFee = withdrawalFee;
	}

	public BigDecimal getInitialBalance() {
		return initialBalance;
	}

	public BigDecimal getMinResidualBalance() {
		return minResidualBalance;
	}
	
	public BigDecimal getMinWithdrawalAmount() {
		return minWithdrawalAmount;
	}

	public String getTag() {
		return tag;
	}

	public BigDecimal getDepositFee() {
		return depositFee;
	}

	public BigDecimal getWithdrawalFee() {
		return withdrawalFee;
	}
	
	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return "Wallet [initialBalance=" + initialBalance + ", minResidualBalance=" + minResidualBalance + ", tag="
				+ tag + ", depositFee=" + depositFee + ", withdrawalFee=" + withdrawalFee + "]";
	}

}

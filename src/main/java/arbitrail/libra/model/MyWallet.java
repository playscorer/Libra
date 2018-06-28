package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class MyWallet {

	@JacksonXmlProperty(localName = "initialBalance")
	private BigDecimal initialBalance;

	@JacksonXmlProperty(localName = "minResidualBalance")
	private BigDecimal minResidualBalance;
	
	@JacksonXmlProperty(localName = "address")
	private String address;

	@JacksonXmlProperty(localName = "tag")
	private String tag;

	@JacksonXmlProperty(localName = "depositFee")
	private BigDecimal depositFee;

	@JacksonXmlProperty(localName = "withdrawalFee")
	private BigDecimal withdrawalFee;
	
	public MyWallet() {
		super();
	}

	public MyWallet(BigDecimal initialBalance) {
		this(initialBalance, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public MyWallet(BigDecimal initialBalance, BigDecimal minResidualBalance, String tag, BigDecimal depositFee, BigDecimal withdrawalFee) {
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
	
	public String getAddress() {
		return address;
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
	
	@Override
	public String toString() {
		return "MyWallet [initialBalance=" + initialBalance + ", minResidualBalance=" + minResidualBalance
				+ ", address=" + address + ", tag=" + tag + ", depositFee=" + depositFee + ", withdrawalFee="
				+ withdrawalFee + "]";
	}

}

package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public final class CurrencyAttribute {
	
	@JacksonXmlProperty(localName = "code", isAttribute = true)
	private String code;

	@JacksonXmlProperty(localName = "test", isAttribute = true)
	private Boolean test;
	
	@JacksonXmlProperty(localName = "maxTestAmount", isAttribute = true)
	private BigDecimal maxTestAmount;

	public CurrencyAttribute() {
	}

	public CurrencyAttribute(String code) {
		super();
		this.code = code;
	}

	public CurrencyAttribute(String code, Boolean test, BigDecimal maxTestAmount) {
		super();
		this.code = code;
		this.test = test;
		this.maxTestAmount = maxTestAmount;
	}

	public String getCode() {
		return code;
	}

	public Boolean isTest() {
		return test;
	}

	public BigDecimal getMaxTestAmount() {
		return maxTestAmount;
	}

	@Override
	public String toString() {
		return "Currency [code=" + code + ", test=" + test + ", maxTestAmount=" + maxTestAmount + "]";
	}

}

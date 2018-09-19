package arbitrail.libra.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public final class CurrencyAttribute {
	
	@JacksonXmlProperty(localName = "code", isAttribute = true)
	private String code;

	@JacksonXmlProperty(localName = "test", isAttribute = true)
	private boolean test;
	
	@JacksonXmlProperty(localName = "testAmount", isAttribute = true)
	private BigDecimal testAmount;

	public CurrencyAttribute() {
	}

	public CurrencyAttribute(String code) {
		super();
		this.code = code;
	}

	public CurrencyAttribute(String code, boolean test, BigDecimal testAmount) {
		super();
		this.code = code;
		this.test = test;
		this.testAmount = testAmount;
	}

	public String getCode() {
		return code;
	}

	public boolean isTest() {
		return test;
	}

	public BigDecimal getTestAmount() {
		return testAmount;
	}

	@Override
	public String toString() {
		return "Currency [code=" + code + ", test=" + test + ", maxTestAmount=" + testAmount + "]";
	}

}

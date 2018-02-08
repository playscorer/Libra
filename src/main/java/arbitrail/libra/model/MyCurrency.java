package arbitrail.libra.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public final class MyCurrency {
	
	@JacksonXmlProperty(localName = "code", isAttribute = true)
	private String code;

	public MyCurrency() {
	}

	public MyCurrency(String code) {
		super();
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	@Override
	public String toString() {
		return "Currency [code=" + code + "]";
	}

}

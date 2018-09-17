package arbitrail.libra.model;

import java.util.Arrays;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "currencies") public final class CurrencyAttributes {
	
	@JacksonXmlElementWrapper(localName = "currency", useWrapping = false)
	private CurrencyAttribute[] currency;

	public CurrencyAttributes() {
	}

	public CurrencyAttributes(CurrencyAttribute[] currency) {
		super();
		this.currency = currency;
	}

	public CurrencyAttribute[] getCurrency() {
		return currency;
	}

	public void setCurrency(CurrencyAttribute[] currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		return "CurrencyAttributes [currency=" + Arrays.toString(currency) + "]";
	}

}

package libra.model;

import java.util.Arrays;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "currencies") public final class Currencies {
	
	@JacksonXmlElementWrapper(localName = "currency", useWrapping = false)
	private MyCurrency[] currency;

	public Currencies() {
	}

	public Currencies(MyCurrency[] currency) {
		super();
		this.currency = currency;
	}

	public MyCurrency[] getCurrency() {
		return currency;
	}

	public void setCurrency(MyCurrency[] currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		return "Currencies [currency=" + Arrays.toString(currency) + "]";
	}

}

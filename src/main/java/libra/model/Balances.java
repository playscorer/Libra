package libra.model;

import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "balances")  public final class Balances {

	@JacksonXmlElementWrapper(localName = "balanceMap", useWrapping = false)
	private Map<String, Map<String, MyBalance>> balanceMap;

	public Balances() {
		super();
	}

	public Balances(Map<String, Map<String, MyBalance>> balanceMap) {
		super();
		this.balanceMap = balanceMap;
	}

	public Map<String, Map<String, MyBalance>> getBalanceMap() {
		return balanceMap;
	}

	public void setBalanceMap(Map<String, Map<String, MyBalance>> balanceMap) {
		this.balanceMap = balanceMap;
	}

	@Override
	public String toString() {
		return "Balances [balanceMap=" + balanceMap + "]";
	}
	
}

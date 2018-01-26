package arbitrail.libra.utils;

import org.knowm.xchange.currency.Currency;

public class ExchCcy {

	private String exchangeName;
	private Currency currency;
	
	public ExchCcy(String exchangeName, Currency currency) {
		super();
		this.exchangeName = exchangeName;
		this.currency = currency;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public Currency getCurrency() {
		return currency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		result = prime * result + ((exchangeName == null) ? 0 : exchangeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExchCcy other = (ExchCcy) obj;
		if (currency == null) {
			if (other.currency != null)
				return false;
		} else if (!currency.equals(other.currency))
			return false;
		if (exchangeName == null) {
			if (other.exchangeName != null)
				return false;
		} else if (!exchangeName.equals(other.exchangeName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ExchCcy [exchangeName=" + exchangeName + ", currency=" + currency + "]";
	}
	
}

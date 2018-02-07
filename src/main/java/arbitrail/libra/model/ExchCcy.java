package arbitrail.libra.model;

public class ExchCcy {

	private String exchangeName;
	private String currencyCode;

	public ExchCcy(String exchangeName, String currencyCode) {
		this.exchangeName = exchangeName;
		this.currencyCode = currencyCode;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currencyCode == null) ? 0 : currencyCode.hashCode());
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
		if (currencyCode == null) {
			if (other.currencyCode != null)
				return false;
		} else if (!currencyCode.equals(other.currencyCode))
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
		return "ExchCcy [exchangeName=" + exchangeName + ", currencyCode=" + currencyCode + "]";
	}

}

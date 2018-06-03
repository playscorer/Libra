package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import arbitrail.libra.model.ExchCcy;

@Entity
@IdClass(ExchCcy.class)
public class PendingTransxEntity {

	@Id private String exchangeName;
	@Id private String currencyCode;

	public PendingTransxEntity() {
	}

	public PendingTransxEntity(String exchangeName, String currencyCode) {
		super();
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
	public String toString() {
		return "PendingTransxEntity [exchangeName=" + exchangeName + ", currencyCode=" + currencyCode + "]";
	}

}

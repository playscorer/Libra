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
	private boolean pending;

	public PendingTransxEntity() {
	}

	public PendingTransxEntity(String exchangeName, String currencyCode, boolean pending) {
		super();
		this.exchangeName = exchangeName;
		this.currencyCode = currencyCode;
		this.pending = pending;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public boolean isPending() {
		return pending;
	}

	@Override
	public String toString() {
		return "PendingTransxEntity [exchangeName=" + exchangeName + ", currencyCode=" + currencyCode + ", pending="
				+ pending + "]";
	}

}

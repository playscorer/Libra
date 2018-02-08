package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import arbitrail.libra.model.ExchCcy;

@Entity @IdClass(ExchCcy.class)
public class PendingTransxEntity {

	@Id private String exchange;
	@Id private String currency;
	private boolean status;

	public PendingTransxEntity() {
	}

	public PendingTransxEntity(String exchange, String currency, boolean status) {
		this.exchange = exchange;
		this.currency = currency;
		this.status = status;
	}

	public String getExchange() {
		return exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public boolean isStatus() {
		return status;
	}

}

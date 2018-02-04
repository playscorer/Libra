package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PendingTransxEntity {

	@Id
	private Integer id;
	private String exchange;
	private String currency;
	private boolean status;

	public PendingTransxEntity() {
	}

	public PendingTransxEntity(Integer id, String exchange, String currency, boolean status) {
		this.id = id;
		this.exchange = exchange;
		this.currency = currency;
		this.status = status;
	}

	public Integer getId() {
		return id;
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

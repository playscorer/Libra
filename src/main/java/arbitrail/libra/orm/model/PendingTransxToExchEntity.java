package arbitrail.libra.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PendingTransxToExchEntity {

	@Id
	private Integer id;
	private String transxId;
	private String exchange;

	public PendingTransxToExchEntity() {
	}

	public PendingTransxToExchEntity(Integer id, String transxId, String exchange) {
		this.id = id;
		this.transxId = transxId;
		this.exchange = exchange;
	}

	public Integer getId() {
		return id;
	}

	public String getTransxId() {
		return transxId;
	}

	public String getExchange() {
		return exchange;
	}

}

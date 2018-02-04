package arbitrail.libra.orm.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class WalletEntity {

	@Id
	private Integer id;
	private String exchange;
	private String currency;
	private BigDecimal lastBalancedAmount;

	public WalletEntity() {
	}

	public WalletEntity(Integer id, String exchange, String currency, BigDecimal lastBalancedAmount) {
		this.id = id;
		this.exchange = exchange;
		this.currency = currency;
		this.lastBalancedAmount = lastBalancedAmount;
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

	public BigDecimal getLastBalancedAmount() {
		return lastBalancedAmount;
	}

}

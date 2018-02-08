package arbitrail.libra.orm.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import arbitrail.libra.model.ExchCcy;

@Entity @IdClass(ExchCcy.class)
public class WalletEntity {

	@Id private String exchange;
	@Id private String currency;
	private BigDecimal lastBalancedAmount;

	public WalletEntity() {
	}

	public WalletEntity(String exchange, String currency, BigDecimal lastBalancedAmount) {
		this.exchange = exchange;
		this.currency = currency;
		this.lastBalancedAmount = lastBalancedAmount;
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

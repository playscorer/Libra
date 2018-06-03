package arbitrail.libra.orm.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import arbitrail.libra.model.ExchCcy;

@Entity
@IdClass(ExchCcy.class)
public class WalletEntity {

	@Id private String exchangeName;
	@Id private String currencyCode;
	private BigDecimal lastBalancedAmount;

	public WalletEntity() {
	}

	public WalletEntity(String exchangeName, String currencyCode, BigDecimal lastBalancedAmount) {
		super();
		this.exchangeName = exchangeName;
		this.currencyCode = currencyCode;
		this.lastBalancedAmount = lastBalancedAmount;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public BigDecimal getLastBalancedAmount() {
		return lastBalancedAmount;
	}

	@Override
	public String toString() {
		return "WalletEntity [exchangeName=" + exchangeName + ", currencyCode=" + currencyCode + ", lastBalancedAmount="
				+ lastBalancedAmount + "]";
	}

}

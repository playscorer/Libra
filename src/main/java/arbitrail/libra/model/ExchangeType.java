package arbitrail.libra.model;

import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.bitfinex.v1.BitfinexExchange;
import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.bittrex.BittrexExchange;
import org.knowm.xchange.cexio.CexIOExchange;
import org.knowm.xchange.hitbtc.v2.HitbtcExchange;

public enum ExchangeType {
	Bitstamp(BitstampExchange.class), 
	Bittrex(BittrexExchange.class), 
	Bitfinex(BitfinexExchange.class), 
	Binance(BinanceExchange.class), 
	Hitbtc(HitbtcExchange.class), 
	Cex(CexIOExchange.class);
	
	private Class<? extends BaseExchange> exchangeClass;

	private ExchangeType(Class<? extends BaseExchange> exchangeClass) {
		this.exchangeClass = exchangeClass;
	}

	public Class<? extends BaseExchange> getExchangeClass() {
		return exchangeClass;
	}

}

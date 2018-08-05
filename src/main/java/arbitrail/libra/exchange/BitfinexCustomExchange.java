package arbitrail.libra.exchange;

import org.knowm.xchange.bitfinex.v1.service.BitfinexMarketDataService;
import org.knowm.xchange.bitfinex.v1.service.BitfinexTradeService;
import org.knowm.xchange.bitfinex.v2.BitfinexExchange;

public class BitfinexCustomExchange extends BitfinexExchange {

	@Override
	protected void initServices() {
		this.marketDataService = new BitfinexMarketDataService(this);
		this.accountService = new BitfinexAccountCustomService(this);
		this.tradeService = new BitfinexTradeService(this);
	}

}

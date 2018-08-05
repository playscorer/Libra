package arbitrail.libra.exchange;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliasCode {
	
	private final static Logger LOG = LoggerFactory.getLogger(AliasCode.class);
	
	private static Map<String, String> aliasTable = new HashMap<>();
	static {
		aliasTable.put("BTC", "BTC");
		aliasTable.put("ETH", "ETH");
		aliasTable.put("XRP", "XRP");
		aliasTable.put("NEO", "NEO");
		aliasTable.put("XVG", "Verge");
		aliasTable.put("XMR", "Monero");
		aliasTable.put("DASH", "DSH");
		aliasTable.put("USDT", "USD");
		aliasTable.put("IOTA", "IOT");
		aliasTable.put("TRX", "TRX");
		aliasTable.put("XLM", "XLM");
		aliasTable.put("LTC", "LTC");
		aliasTable.put("EOS", "EOS");
		aliasTable.put("QTUM", "QTM");
	}

	private static Map<String, String> reverseAliasTable = new HashMap<>();
	static {
		reverseAliasTable.put("BTC", "BTC");
		reverseAliasTable.put("ETH", "ETH");
		reverseAliasTable.put("XRP", "XRP");
		reverseAliasTable.put("NEO", "NEO");
		reverseAliasTable.put("Verge", "XVG");
		reverseAliasTable.put("Monero", "XMR");
		reverseAliasTable.put("DSH", "DASH");
		reverseAliasTable.put("USD", "USDT");
		reverseAliasTable.put("IOT", "IOTA");
		reverseAliasTable.put("TRX", "TRX");
		reverseAliasTable.put("XLM", "XLM");
		reverseAliasTable.put("LTC", "LTC");
		reverseAliasTable.put("EOS", "EOS");
		reverseAliasTable.put("QTM", "QTUM");
	}
	
	public static String getBitfinexCode(String currencyCode) {
		if (!aliasTable.containsKey(currencyCode)) {
			LOG.error("There is no alias for Bitfinex in the table for : " + currencyCode);
			return null;
		}
		return aliasTable.get(currencyCode);
	}
	
	public static String getGenericCode(String aliasCode) {
		if (!reverseAliasTable.containsKey(aliasCode)) {
			LOG.error("There is no generic alias defined in the table for : " + aliasCode);
			return null;
		}
		return reverseAliasTable.get(aliasCode);		
	}

}

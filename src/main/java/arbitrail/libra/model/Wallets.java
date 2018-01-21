package arbitrail.libra.model;

import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "wallets")  public final class Wallets {

	@JacksonXmlElementWrapper(localName = "walletMap", useWrapping = false)
	private Map<String, Map<String, Wallet>> walletMap;

	public Wallets() {
		super();
	}

	public Wallets(Map<String, Map<String, Wallet>> walletMap) {
		super();
		this.walletMap = walletMap;
	}

	public Map<String, Map<String, Wallet>> getWalletMap() {
		return walletMap;
	}

	public void setWalletMap(Map<String, Map<String, Wallet>> walletMap) {
		this.walletMap = walletMap;
	}

	@Override
	public String toString() {
		return "Wallets [walletMap=" + walletMap + "]";
	}
	
}

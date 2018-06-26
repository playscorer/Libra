package arbitrail.libra.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public final class Account {
	
	@JacksonXmlProperty(localName = "name", isAttribute = true)
	private String name;
	@JacksonXmlProperty(localName = "apiKey")
	private String apiKey;
	@JacksonXmlProperty(localName = "key")
	private String key;
	@JacksonXmlProperty(localName = "wallet")
	private String wallet;
	
	public Account() {
		super();
	}

	public Account(String name) {
		super();
		this.name = name;
	}

	public Account(String name, String apiKey, String key, String wallet) {
		super();
		this.name = name;
		this.apiKey = apiKey;
		this.key = key;
		this.wallet = wallet;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getWallet() {
		return wallet;
	}

	public void setWallet(String wallet) {
		this.wallet = wallet;
	}

	@Override
	public String toString() {
		return "Account [name=" + name + ", apiKey=" + apiKey + ", key=" + key + ", wallet=" + wallet + "]";
	}
	
}

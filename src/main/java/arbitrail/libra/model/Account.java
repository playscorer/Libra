package arbitrail.libra.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public final class Account {
	
	@JacksonXmlProperty(localName = "name", isAttribute = true)
	private String name;
	@JacksonXmlProperty(localName = "username")
	private String username;
	@JacksonXmlProperty(localName = "apiKey")
	private String apiKey;
	@JacksonXmlProperty(localName = "key")
	private String key;
	
	public Account() {
		super();
	}

	public Account(String name) {
		super();
		this.name = name;
	}

	public Account(String name, String username, String apiKey, String key) {
		super();
		this.name = name;
		this.username = username;
		this.apiKey = apiKey;
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	@Override
	public String toString() {
		return "Account [name=" + name + ", username=" + username + ", apiKey=" + apiKey + ", key=" + key + "]";
	}
	
}

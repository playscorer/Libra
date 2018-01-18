package arbitrail.libra.model;

import java.util.Arrays;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "accounts") public final class Accounts {

	@JacksonXmlElementWrapper(localName = "account", useWrapping = false)
	private Account[] account;

	public Accounts() {
		super();
	}

	public Accounts(Account[] account) {
		super();
		this.account = account;
	}

	public Account[] getAccount() {
		return account;
	}

	public void setAccount(Account[] account) {
		this.account = account;
	}

	@Override
	public String toString() {
		return "Accounts [account=" + Arrays.toString(account) + "]";
	}
	
}

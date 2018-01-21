package arbitrail.libra.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import arbitrail.libra.model.Accounts;
import arbitrail.libra.model.Wallets;
import arbitrail.libra.model.Currencies;


public class Parser {
	
	public static final String CURRENCIES_FILENAME = "src/main/resources/data/currencies.xml";
	public static final String ACCOUNTS_FILENAME = "src/main/resources/data/accounts.xml";
	public static final String WALLETS_FILENAME = "src/main/resources/data/output/wallets.xml";

	public static Currencies parseCurrencies() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new XmlMapper();
        Currencies currencies = objectMapper.readValue(
                StringUtils.toEncodedString(Files.readAllBytes(Paths.get(CURRENCIES_FILENAME)), StandardCharsets.UTF_8),
                Currencies.class);
        return currencies;
	}
	
	public static Accounts parseAccounts() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new XmlMapper();
        Accounts accounts = objectMapper.readValue(
                StringUtils.toEncodedString(Files.readAllBytes(Paths.get(ACCOUNTS_FILENAME)), StandardCharsets.UTF_8),
                Accounts.class);
        return accounts;
	}
	
	public static Wallets parseWallets() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new XmlMapper();
		Wallets balances = objectMapper.readValue(
				StringUtils.toEncodedString(Files.readAllBytes(Paths.get(WALLETS_FILENAME)), StandardCharsets.UTF_8),
				Wallets.class);
		return balances;
	}
	
	public static void saveAccountsBalanceToFile(Wallets balances) throws JsonGenerationException, JsonMappingException, IOException {
		Files.deleteIfExists(Paths.get(WALLETS_FILENAME));
		ObjectMapper objectMapper = new XmlMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(Paths.get(WALLETS_FILENAME).toFile(), balances);
	}
	
}

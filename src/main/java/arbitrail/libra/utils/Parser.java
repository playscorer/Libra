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
import arbitrail.libra.model.Balances;
import arbitrail.libra.model.Currencies;


public class Parser {
	
	public static final String CURRENCIES_FILENAME = "src/main/resources/data/currencies.xml";
	public static final String ACCOUNTS_FILENAME = "src/main/resources/data/accounts.xml";
	public static final String BALANCES_FILENAME = "src/main/resources/data/output/balances.xml";

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
	
	public static Balances parseBalances() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new XmlMapper();
		Balances balances = objectMapper.readValue(
				StringUtils.toEncodedString(Files.readAllBytes(Paths.get(BALANCES_FILENAME)), StandardCharsets.UTF_8),
				Balances.class);
		return balances;
	}
	
	public static void saveAccountsBalanceToFile(Balances balances) throws JsonGenerationException, JsonMappingException, IOException {
		Files.deleteIfExists(Paths.get(BALANCES_FILENAME));
		ObjectMapper objectMapper = new XmlMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(Paths.get(BALANCES_FILENAME).toFile(), balances);
	}
	
}

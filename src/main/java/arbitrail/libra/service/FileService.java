package arbitrail.libra.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import arbitrail.libra.model.Accounts;
import arbitrail.libra.model.CurrencyAttributes;
import arbitrail.libra.model.Wallets;

@Component
public class FileService {
	
	@Value( "${currencies_filepath}" )
	private String CURRENCIES_FILEPATH;
	
	@Value( "${accounts_filepath}" )
	private String ACCOUNTS_FILEPATH;
	
	@Value( "${wallets_filepath}" )
	private String WALLETS_FILEPATH;

	public CurrencyAttributes parseCurrencies() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new XmlMapper();
        CurrencyAttributes currencies = objectMapper.readValue(
                StringUtils.toEncodedString(Files.readAllBytes(Paths.get(CURRENCIES_FILEPATH)), StandardCharsets.UTF_8),
                CurrencyAttributes.class);
        return currencies;
	}
	
	public Accounts parseAccounts() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new XmlMapper();
        Accounts accounts = objectMapper.readValue(
                StringUtils.toEncodedString(Files.readAllBytes(Paths.get(ACCOUNTS_FILEPATH)), StandardCharsets.UTF_8),
                Accounts.class);
        return accounts;
	}
	
	public boolean existsWalletsFile() {
		return Files.exists(Paths.get(WALLETS_FILEPATH));
	}
	
	public Wallets parseWallets() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new XmlMapper();
		Wallets balances = objectMapper.readValue(
				StringUtils.toEncodedString(Files.readAllBytes(Paths.get(WALLETS_FILEPATH)), StandardCharsets.UTF_8),
				Wallets.class);
		return balances;
	}
	
	public void saveWalletsToFile(Wallets balances) throws JsonGenerationException, JsonMappingException, IOException {
		Files.deleteIfExists(Paths.get(WALLETS_FILEPATH));
		ObjectMapper objectMapper = new XmlMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(Paths.get(WALLETS_FILEPATH).toFile(), balances);
	}
	
}

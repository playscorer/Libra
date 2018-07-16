package arbitrail.libra.orm.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arbitrail.libra.model.ExchangeType;
import arbitrail.libra.model.MyWallet;
import arbitrail.libra.orm.dao.WalletDao;
import arbitrail.libra.orm.model.WalletEntity;

@Service
public class WalletService {
	
	private final static Logger LOG = Logger.getLogger(WalletService.class);
	
	@Value( "${percent_fee}" )
	private BigDecimal percentFee;

	@Value( "${percent_balance}" )
	private BigDecimal percentBalance;
	
	@Autowired
	private WalletDao walletDao;

	@Transactional
	public void save(WalletEntity wallet) {
		walletDao.saveOrUpdate(wallet);
	}
	
	@Transactional(readOnly = true)
	public BigDecimal getLastBalancedAmount(String exchange, String currency) {
		WalletEntity entity = walletDao.find(exchange, currency);
		return entity == null ? BigDecimal.ZERO : entity.getLastBalancedAmount() == null ? BigDecimal.ZERO : entity.getLastBalancedAmount();
	}	
	
	@Transactional(readOnly = true)
	public List<WalletEntity> listAll() {
		return walletDao.findAll();

	}
	
	/**
	 * Handles single and multiple wallets per currency and exclude the zero balance
	 * in order to avoid synchronization bugs with the exchange.
	 */
	public BigDecimal getAvailableBalance(Exchange exchange, String walletId, Currency currency) throws IOException {
		// find the wallet depending if it is an exchange with multiple wallets per currency
		Wallet wallet;
		if (walletId == null || walletId.isEmpty()) {
			wallet = exchange.getAccountService().getAccountInfo().getWallet();
		} else {
			wallet = exchange.getAccountService().getAccountInfo().getWallet(walletId);
		}
		// return the available balance if it is not zero
		Balance balance = wallet.getBalance(currency);
		return balance.getAvailable();
	}

	/**
	 * Handles single and multiple wallets for all currencies of an exchange.
	 */
	public Map<Currency, Balance> getAvailableBalances(Exchange exchange, String walletId) throws IOException {
		// find the wallet depending if it is an exchange with multiple wallets per currency
		Wallet wallet;
		if (walletId == null || walletId.isEmpty()) {
			wallet = exchange.getAccountService().getAccountInfo().getWallet();
		} else {
			wallet = exchange.getAccountService().getAccountInfo().getWallet(walletId);
		}
		// return the available balances for all currencies of an exchange
		return wallet.getBalances();
	}
	
	/**
	 * Returns the deposit address for the specified wallet : for Bittrex with XRP it looks into the file config. 
	 */
	public String getDepositAddress(Exchange toExchange, String exchangeName, MyWallet toWallet, Currency currency) throws IOException {
		String depositAddress;
		if (ExchangeType.Bittrex.name().equals(exchangeName) && Currency.XRP.equals(currency)) {
			depositAddress = toWallet.getAddress();
			if (depositAddress == null) {
				LOG.warn("Configuration error : Deposit address required for Bittrex with XRP");
			}
		} else {
			depositAddress = toExchange.getAccountService().requestDepositAddress(currency);
		}
		// specific case for XRP and Bitstamp
/*		if (Currency.XRP.equals(fundingRecord.getCurrency()) && ExchangeType.Bitstamp.name().equals(exchangeName)) {
			depositAddress = ((BitstampAccountService) exchange.getAccountService()).getRippleDepositAddress().getAddress();
		}*/
		return depositAddress;
	}
	
	/**
	 * The minWithdrawalAmount is the max(percent_balance * sumBalances / nbExchanges, fees / percentFee).
	 */
	public BigDecimal getMinWithdrawalAmount(MyWallet fromWallet, MyWallet toWallet, Currency currency, Map<Exchange, Map<Currency, Balance>> balanceMap) {
		BigDecimal sumBalances = BigDecimal.ZERO;
		int nbExchanges = 0;
		
		for (Entry<Exchange,Map<Currency,Balance>> entry : balanceMap.entrySet()) {
			Balance balance = entry.getValue().get(currency);
			BigDecimal balanceAvailable = balance == null ? BigDecimal.ZERO : balance.getAvailable();
			sumBalances = sumBalances.add(balanceAvailable);
			nbExchanges++;
		}
		sumBalances = percentBalance.multiply(sumBalances).divide(new BigDecimal(nbExchanges));
		BigDecimal fees = fromWallet.getWithdrawalFee().add(toWallet.getDepositFee());
		return sumBalances.max(fees.divide(percentFee));
	}
}

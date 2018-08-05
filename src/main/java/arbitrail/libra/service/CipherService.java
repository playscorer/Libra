package arbitrail.libra.service;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CipherService {

    private final Logger LOG = LoggerFactory.getLogger(CipherService.class);

    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String KEY_ALGORITHM = "AES";
    public static final String PASS_HASH_ALGORITHM = "SHA-256";
    
    @Value( "${salt}" )
    private String salt;

    @Value( "${iv}" )
    private String iv;

    public String encrypt(String data, String password) {
        try {
            Cipher cipher = buildCipher(password, Cipher.ENCRYPT_MODE);
            byte[] dataToSend = data.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedData = cipher.doFinal(dataToSend);
            return Base64.encodeBase64URLSafeString(encryptedData);

        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encryptedValue, String password) {
        try {
            Cipher cipher = buildCipher(password, Cipher.DECRYPT_MODE);
            byte[] encryptedData = Base64.decodeBase64(encryptedValue);
            byte[] data = cipher.doFinal(encryptedData);
            return new String(data, StandardCharsets.UTF_8);

        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Cipher buildCipher(String password, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        Key key = buildKey(password);
        cipher.init(mode, key, ivParameterSpec);
        return cipher;
    }

    private Key buildKey(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String passwordHash = sha256(sha256(password) + salt);
		byte[] key = hexStringToByteArray(passwordHash);
		return new SecretKeySpec(key, KEY_ALGORITHM);
    }
    
	private String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance(PASS_HASH_ALGORITHM);
			byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
    
}
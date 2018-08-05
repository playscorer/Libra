package arbitrail.libra.exchange;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class BitfinexHttpTest {
	
	private final static Logger LOG = Logger.getLogger(BitfinexHttpTest.class);

    private static final String ALGORITHM_HMACSHA384 = "HmacSHA384";

    private String apiKey = "";
    private String apiKeySecret = "";
    private long nonce = System.currentTimeMillis();

    /**
     * public access only
     */
    public BitfinexHttpTest() {
        apiKey = null;
        apiKeySecret = null;
    }

    /**
     * public and authenticated access
     *
     * @param apiKey
     * @param apiKeySecret
     */
    public BitfinexHttpTest(String apiKey, String apiKeySecret) {
        this.apiKey = apiKey;
        this.apiKeySecret = apiKeySecret;
    }

    /**
     * Creates an authenticated request WITHOUT request parameters. Send a request for Balances.
     *
     * @return Response string if request successfull
     * @throws IOException
     */
    public String sendRequestV1DepositWithdrawHistory() throws IOException {
        String sResponse;

        HttpURLConnection conn = null;

        String urlPath = "/v1/history/movements";
        // String method = "GET";
        String method = "POST";

        try {
            URL url = new URL("https://api.bitfinex.com" + urlPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);

            if (isAccessPublicOnly()) {
                String msg = "Authenticated access not possible, because key and secret was not initialized: use right constructor.";
                LOG.error(msg);
                return msg;
            }

            conn.setDoOutput(true);
            conn.setDoInput(true);

            JSONObject jo = new JSONObject();
            jo.put("request", urlPath);
            jo.put("nonce", Long.toString(getNonce()));
            jo.put("currency", "NANO");

            // API v1
            String payload = jo.toString();

			// this is usage for Base64 Implementation in Android. For pure java you can use java.util.Base64.Encoder
          // Base64.NO_WRAP: Base64-string have to be as one line string
            String payload_base64 = Base64.getEncoder().encodeToString(payload.getBytes());

			
            String payload_sha384hmac = hmacDigest(payload_base64, apiKeySecret, ALGORITHM_HMACSHA384);

           conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.addRequestProperty("X-BFX-APIKEY", apiKey);
            conn.addRequestProperty("X-BFX-PAYLOAD", payload_base64);
            conn.addRequestProperty("X-BFX-SIGNATURE", payload_sha384hmac);

            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            return convertStreamToString(in);

        } catch (MalformedURLException e) {
            throw new IOException(e.getClass().getName(), e);
        } catch (ProtocolException e) {
            throw new IOException(e.getClass().getName(), e);
        } catch (IOException e) {

            String errMsg = e.getLocalizedMessage();

            if (conn != null) {
                try {
                    sResponse = convertStreamToString(conn.getErrorStream());
                    errMsg += " -> " + sResponse;
                    LOG.error(errMsg, e);
                    return sResponse;
                } catch (IOException e1) {
                    errMsg += " Error on reading error-stream. -> " + e1.getLocalizedMessage();
                    LOG.error(errMsg, e);
                    throw new IOException(e.getClass().getName(), e1);
                }
            } else {
                throw new IOException(e.getClass().getName(), e);
            }
        } catch (JSONException e) {
            String msg = "Error on setting up the connection to server";
            throw new IOException(msg, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public long getNonce() {
        return ++nonce;
    }

    public boolean isAccessPublicOnly() {
        return apiKey == null;
    }

    public static String hmacDigest(String msg, String keyString, String algo) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(key);

            byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

            StringBuffer hash = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (UnsupportedEncodingException e) {
        	LOG.error("Exception: " + e.getMessage());
        } catch (InvalidKeyException e) {
        	LOG.error("Exception: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
        	LOG.error("Exception: " + e.getMessage());
        }
        return digest;
    }
    
    public static void main(String[] args) throws IOException {
    	BitfinexHttpTest test = new BitfinexHttpTest("EY69hcJyb5pHmipUwElLODaqbL3yBRlzqXFTksFcKo2", "S3WY76rGQgvxYijS16z4uRv86kAVjm85xRpgPbovaLF");
    	String history = test.sendRequestV1DepositWithdrawHistory();
    	System.out.println("Deposit/Withdrawal history: " + history);
    }
}

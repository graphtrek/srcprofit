package co.grtk.srcprofit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

@Configuration
public class RestClientConfig {
    private final Environment environment;

    public RestClientConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "ibkrRestClient")
    public RestClient ibkrRestClient() {
        disableSSLCertificateValidation();
        return RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(environment.getRequiredProperty("IBKR_DATA_URL")).build();
    }

    @Bean(name = "ibkrFlexRestClient")
    public RestClient ibkrFlexRestClient() {
        return RestClient.builder()
                .defaultHeader("Accept", MediaType.APPLICATION_XML_VALUE)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    @Bean(name = "alpacaRestClient")
    public RestClient alpacaRestClient() {
        return RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader("APCA-DATA-URL", environment.getRequiredProperty("ALPACA_DATA_URL"))
                .defaultHeader("APCA-API-KEY-ID", environment.getRequiredProperty("ALPACA_API_KEY"))
                .defaultHeader("APCA-API-SECRET-KEY", environment.getRequiredProperty("ALPACA_API_SECRET_KEY"))
                .baseUrl("https://data.alpaca.markets").build();
    }

    @Bean(name = "alphaVintageRestClient")
    public RestClient alphaVintageRestClient() {
        return RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultUriVariables(Map.of("apiKey", environment.getRequiredProperty("ALPHA_VINTAGE_API_KEY")))
                .baseUrl("https://www.alphavantage.co").build();
    }
    /**
     * Should strictly be used only in the local environment.
     */
    private void disableSSLCertificateValidation() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
            }

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        try {
            sslContext.init(null, trustManagers, null);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}

package co.grtk.srcprofit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class IbRestClientConfig {
    Environment environment;

    @Bean(name = "ibkrRestClient")
    public RestClient ibkrRestClient() {
        disableSSLCertificateValidation();
        return RestClient.builder().requestFactory(
                new SimpleClientHttpRequestFactory())
                .baseUrl("https://localhost:5055/").build();
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

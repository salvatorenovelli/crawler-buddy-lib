package com.myseotoolbox.crawler.httpclient;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;


@Log4j2
public class NoSSLVerificationConnectionFactory implements ConnectionFactory {

    // Create a trust manager that does not validate certificate chains
    private static final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    // Create all-trusting host name verifier
    private static final HostnameVerifier allHostsValid = (hostname, session) -> true;
    private final SSLContext sslContext;

    @SneakyThrows
    public NoSSLVerificationConnectionFactory() {
        // Install the all-trusting trust manager
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    }

    @Override
    public HttpURLConnection createConnection(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        if (connection instanceof HttpsURLConnection) {
            disableSslVerificationOn((HttpsURLConnection) connection);
        }
        return connection;
    }

    private void disableSslVerificationOn(HttpsURLConnection connection) {
        // Install the all-trusting host verifier
        connection.setSSLSocketFactory(sslContext.getSocketFactory()); // NET::ERR_CERT_DATE_INVALID
        connection.setHostnameVerifier(allHostsValid); // NET::ERR_CERT_COMMON_NAME_INVALID
    }

}
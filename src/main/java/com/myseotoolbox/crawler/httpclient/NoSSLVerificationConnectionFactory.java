package com.myseotoolbox.crawler.httpclient;

import lombok.extern.log4j.Log4j2;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;


@Log4j2
public class NoSSLVerificationConnectionFactory {

    public HttpURLConnection createConnection(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        if (connection instanceof HttpsURLConnection) {
            disableSslVerificationOn((HttpsURLConnection) connection);
        }
        return connection;
    }

    private static void disableSslVerificationOn(HttpsURLConnection connection) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
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

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            connection.setSSLSocketFactory(sc.getSocketFactory()); // NET::ERR_CERT_DATE_INVALID
            connection.setHostnameVerifier(allHostsValid); // NET::ERR_CERT_COMMON_NAME_INVALID
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.warn("Unable to disable SSL verification for {}. {}", connection.getURL(), e.toString());
        }
    }
}


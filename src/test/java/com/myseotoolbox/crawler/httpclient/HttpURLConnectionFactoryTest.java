package com.myseotoolbox.crawler.httpclient;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class HttpURLConnectionFactoryTest {

    NoSSLVerificationConnectionFactory sut = new NoSSLVerificationConnectionFactory();


    @Test
    public void shouldDisableSSLVerification() {


        Stream<String> urls = Stream.of(
                "https://expired.badssl.com/",
                "https://wrong.host.badssl.com/",
                "https://self-signed.badssl.com/",
                "https://untrusted-root.badssl.com/",
                "https://revoked.badssl.com/");


        urls.forEach(url -> {
            try {
                System.out.println("Connecting to: " + url);
                int status = performGetRequest(url);
                assertThat(status, is(200));
            } catch (IOException e) {
                fail("Unable to connect to: " + url + " " + e.toString());
            }
        });


    }

    private int performGetRequest(String url) throws IOException {
        HttpURLConnection connection = sut.createConnection(URI.create(url));
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }
}

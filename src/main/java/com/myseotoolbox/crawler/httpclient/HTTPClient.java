package com.myseotoolbox.crawler.httpclient;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;


public class HTTPClient {
    public String get(URI uri) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(uri);
            CloseableHttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException(new IOException("Non 200 status code: "+response.getStatusLine().getStatusCode()));
            }
            final HttpEntity entity = response.getEntity();
            return IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
        }
    }
}

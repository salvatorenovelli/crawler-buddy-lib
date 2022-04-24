package com.myseotoolbox.crawler.httpclient;

import java.net.URI;

public class HttpRequestFactory {
    private final HttpURLConnectionFactory connectionFactory;
    public HttpRequestFactory(HttpURLConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    public HttpGetRequest buildGetFor(URI uri) {
        return new HttpGetRequest(uri, connectionFactory);
    }
}

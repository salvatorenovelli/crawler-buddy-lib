package com.myseotoolbox.crawler.httpclient;

import java.net.URI;

public class HttpRequestFactory {
    private final ConnectionFactory connectionFactory;
    public HttpRequestFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    public HttpGetRequest buildGetFor(URI uri) {
        return new HttpGetRequest(uri, connectionFactory);
    }
}

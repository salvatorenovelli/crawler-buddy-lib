package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.HttpRequestFactory;
import com.myseotoolbox.crawler.httpclient.WebPageReader;

public class WebPageReaderFactory {
    private final HttpRequestFactory httpRequestFactory;

    public WebPageReaderFactory(HttpRequestFactory httpRequestFactory) {
        this.httpRequestFactory = httpRequestFactory;
    }

    public WebPageReader build(UriFilter uriFilter) {
        return new WebPageReader(uriFilter, httpRequestFactory);
    }
}

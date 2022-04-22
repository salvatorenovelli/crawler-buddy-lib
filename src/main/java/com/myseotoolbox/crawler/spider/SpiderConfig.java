package com.myseotoolbox.crawler.spider;


import com.myseotoolbox.crawler.httpclient.HttpRequestFactory;
import com.myseotoolbox.crawler.httpclient.HttpURLConnectionFactory;
import com.myseotoolbox.crawler.spider.sitemap.SitemapReader;

public class SpiderConfig {


    private HttpURLConnectionFactory connectionFactory = new HttpURLConnectionFactory();
    private HttpRequestFactory httpRequestFactory = new HttpRequestFactory(connectionFactory);

    public HttpRequestFactory getHttpRequestFactory() {
        return httpRequestFactory;
    }

    public CrawlExecutorFactory getExecutorBuilder() {
        return new CrawlExecutorFactory();
    }

    public CrawlJobFactory getCrawlJobFactory(CrawlExecutorFactory crawlExecutorFactory, SitemapReader sitemapReader) {
        return new CrawlJobFactory(new WebPageReaderFactory(httpRequestFactory), new WebsiteUriFilterFactory(), crawlExecutorFactory, sitemapReader);
    }
}

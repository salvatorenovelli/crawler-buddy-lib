package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.HttpRequestFactory;
import com.myseotoolbox.crawler.httpclient.NoSSLVerificationConnectionFactory;
import com.myseotoolbox.crawler.spider.configuration.CrawlJobConfiguration;
import com.myseotoolbox.crawler.spider.sitemap.SitemapReader;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class CrawlJobBuilder {
    private final URI origin;
    private final CrawlEventListener listener;
    private List<URI> seeds = Collections.emptyList();
    private ThreadPoolExecutorFactory threadPoolExecutorFactory;
    private int maxConcurrentConnections = 1;

    public CrawlJobBuilder(URI origin, CrawlEventListener listener) {
        this.origin = origin;
        this.listener = listener;
    }

    public static CrawlJobBuilder newCrawlJobFor(URI origin, CrawlEventListener listener){
        return new CrawlJobBuilder(origin, listener);
    }

    public CrawlJobBuilder withSeeds(List<URI> seeds) {
        this.seeds = Collections.unmodifiableList(seeds);
        return this;
    }

    public CrawlJob build() {

        NoSSLVerificationConnectionFactory connectionFactory = new NoSSLVerificationConnectionFactory();
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(connectionFactory);
        WebPageReaderFactory webPageReaderFactory = new WebPageReaderFactory(httpRequestFactory);
        SitemapReader sitemapReader = new SitemapReader();

        CrawlJobFactory crawlJobFactory =  new CrawlJobFactory(webPageReaderFactory, new UriFilterFactory(), threadPoolExecutorFactory, sitemapReader);


        maxConcurrentConnections = 1;
        CrawlJobConfiguration conf = CrawlJobConfiguration
                .newConfiguration(origin)
                .withSeeds(seeds)
                .withConcurrentConnections(maxConcurrentConnections)
                .withRobotsTxt(RobotsTxtBuilder.buildRobotsTxtForOrigin(origin, false))
                .build();

        return crawlJobFactory.build(conf, listener);
    }

    public CrawlJobBuilder withThreadPoolFactory(ThreadPoolExecutorFactory factory) {
        this.threadPoolExecutorFactory = factory;
        return this;
    }

    public CrawlJobBuilder withConcurrentConnections(int maxConcurrentConnections) {
        this.maxConcurrentConnections = maxConcurrentConnections;
        return this;
    }
}

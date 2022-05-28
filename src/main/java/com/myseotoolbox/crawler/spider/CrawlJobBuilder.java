package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.ConnectionFactory;
import com.myseotoolbox.crawler.httpclient.HttpRequestFactory;
import com.myseotoolbox.crawler.httpclient.NoSSLVerificationConnectionFactory;
import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.spider.configuration.AllowedPathFromSeeds;
import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
import com.myseotoolbox.crawler.spider.sitemap.SitemapReader;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrawlJobBuilder {
    private final URI origin;
    private final CrawlEventListener listener;
    private List<URI> seeds = Collections.emptyList();
    private CrawlerThreadPoolExecutorFactory threadPoolExecutorFactory = new CrawlerThreadPoolExecutorFactory();
    private int maxConcurrentConnections = 1;
    private int crawlLimit = 10000;
    private CrawlJobBuilder(URI origin, CrawlEventListener listener) {
        this.origin = origin;
        this.listener = listener;
    }

    public static CrawlJobBuilder newCrawlJobFor(URI origin, CrawlEventListener listener) {
        return new CrawlJobBuilder(origin, listener);
    }

    public CrawlJobBuilder withSeeds(List<URI> seeds) {
        this.seeds = Collections.unmodifiableList(seeds);
        return this;
    }

    public CrawlJobBuilder withThreadPoolFactory(CrawlerThreadPoolExecutorFactory factory) {
        this.threadPoolExecutorFactory = factory;
        return this;
    }

    public CrawlJobBuilder withConcurrentConnections(int maxConcurrentConnections) {
        this.maxConcurrentConnections = maxConcurrentConnections;
        return this;
    }

    public CrawlJobBuilder withCrawlLimit(int limit) {
        this.crawlLimit = limit;
        return this;
    }

    public CrawlJob build() {

        List<String> allowedPaths = AllowedPathFromSeeds.extractAllowedPathFromSeeds(seeds);
        RobotsTxt robotsTxt = RobotsTxtBuilder.buildRobotsTxtForOrigin(origin, false);
        UriFilter uriFilter = new DefaultUriFilter(origin, allowedPaths, robotsTxt);

        ConnectionFactory connectionFactory = new NoSSLVerificationConnectionFactory();
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(connectionFactory);

        WebPageReader webPageReader = new WebPageReader(uriFilter, httpRequestFactory);

        SitemapReader sitemapReader = new SitemapReader();
        List<URI> seedsFromSitemap = sitemapReader.getSeedsFromSitemaps(origin, robotsTxt.getSitemaps(), uriFilter);

        ThreadPoolExecutor executor = threadPoolExecutorFactory.buildThreadPool(origin.getHost(), maxConcurrentConnections);
        List<URI> allSeeds = concatCollections(seeds, seedsFromSitemap);

        return new CrawlJob(origin, allSeeds, webPageReader, uriFilter, executor, crawlLimit, listener);
    }

    private List<URI> concatCollections(Collection<URI> seeds, Collection<URI> seedsFromSitemap) {
        return Stream.concat(seeds.stream(), seedsFromSitemap.stream()).collect(Collectors.toList());
    }
}

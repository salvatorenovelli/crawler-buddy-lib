package com.myseotoolbox.crawler.spider;

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
    private ThreadPoolExecutorFactory threadPoolExecutorFactory;
    private int maxConcurrentConnections = 1;

    private int crawlLimit = 1;

    public CrawlJobBuilder(URI origin, CrawlEventListener listener) {
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

    public CrawlJob build() {

        NoSSLVerificationConnectionFactory connectionFactory = new NoSSLVerificationConnectionFactory();
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(connectionFactory);
        WebPageReaderFactory webPageReaderFactory = new WebPageReaderFactory(httpRequestFactory);
        SitemapReader sitemapReader = new SitemapReader();

        UriFilterFactory uriFilterFactory = new UriFilterFactory();

        List<String> allowedPaths = AllowedPathFromSeeds.extractAllowedPathFromSeeds(seeds);

        RobotsTxt robotsTxt = RobotsTxtBuilder.buildRobotsTxtForOrigin(origin, false);
        //any changes to this filter needs to be duplicated in the sitemap filtering (for now is duplicated logic)
        UriFilter uriFilter = uriFilterFactory.build(origin, allowedPaths, robotsTxt);
        WebPageReader webPageReader = webPageReaderFactory.build(uriFilter);
        ThreadPoolExecutor executor = threadPoolExecutorFactory.buildThreadPool(origin.getHost(), maxConcurrentConnections);

        List<URI> seedsFromSitemap = sitemapReader.getSeedsFromSitemaps(origin, robotsTxt.getSitemaps(), allowedPaths);

        List<URI> allSeeds = concat(seeds, seedsFromSitemap);

        return new CrawlJob(origin, allSeeds, webPageReader, uriFilter, executor, crawlLimit, listener);
    }

    public CrawlJobBuilder withThreadPoolFactory(ThreadPoolExecutorFactory factory) {
        this.threadPoolExecutorFactory = factory;
        return this;
    }

    public CrawlJobBuilder withConcurrentConnections(int maxConcurrentConnections) {
        this.maxConcurrentConnections = maxConcurrentConnections;
        return this;
    }


    private List<URI> concat(Collection<URI> seeds, Collection<URI> seedsFromSitemap) {
        return Stream.concat(seeds.stream(), seedsFromSitemap.stream()).collect(Collectors.toList());
    }

    public CrawlJobBuilder withCrawlLimit(int i) {
        this.crawlLimit= i;
        return this;
    }
}

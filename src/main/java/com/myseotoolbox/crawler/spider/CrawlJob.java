package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.isHostMatching;

@Slf4j
public class CrawlJob {

    private final CrawlerQueue crawlerQueue;

    public CrawlJob(String name, Collection<URI> seeds, WebPageReader pageReader, UriFilter uriFilter, ThreadPoolExecutor executor, int maxCrawls) {
        verifySameOrigin(seeds);
        CrawlersPool pool = new CrawlersPool(pageReader, executor);
        this.crawlerQueue = new CrawlerQueue(name, seeds, pool, uriFilter, maxCrawls);
        startMonitoring(name, executor);
    }


    private void startMonitoring(String name, ThreadPoolExecutor executor) {
        new CrawlerPoolStatusMonitor(name, executor).start();
    }

    public void subscribeToPageCrawled(Consumer<PageSnapshot> subscriber) {
        this.crawlerQueue.subscribeToPageCrawled(subscriber);
    }

    public void start() {
        crawlerQueue.start();
    }


    private void verifySameOrigin(Collection<URI> seeds) {
        if (seeds.size() > 0) {
            URI websiteOrigin = WebsiteOriginUtils.extractRoot(seeds.iterator().next());//takes the first one
            if (seeds.stream().anyMatch(uri -> !isHostMatching(websiteOrigin, uri)))
                throw new IllegalStateException("Seeds host must match website origin. Origin: " + websiteOrigin + " Seeds:" + seeds);
        }
    }

}



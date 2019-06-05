package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class CrawlJob {

    private final CrawlerQueue crawlerQueue;

    public CrawlJob(URI origin, Collection<URI> seeds, WebPageReader pageReader, UriFilter uriFilter, ThreadPoolExecutor executor, int maxCrawls) {
        String name = origin.getHost();
        CrawlersPool pool = new CrawlersPool(pageReader, executor);
        this.crawlerQueue = new CrawlerQueue(name, removeSeedsOutsideOrigin(origin, seeds), pool, uriFilter, maxCrawls);
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

    private List<URI> removeSeedsOutsideOrigin(URI origin, Collection<URI> seeds) {
        List<URI> filtered = seeds.stream().filter(u -> WebsiteOriginUtils.isHostMatching(origin, u)).collect(Collectors.toList());
        if (filtered.size() != seeds.size())
            log.warn("Seeds from external domains found on {}. Original Seeds: {} Filtered Seeds: {}", origin, seeds.size(), filtered.size());
        return filtered;
    }

}



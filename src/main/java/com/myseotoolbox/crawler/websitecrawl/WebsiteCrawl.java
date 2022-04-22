package com.myseotoolbox.crawler.websitecrawl;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collection;


@Data
public class WebsiteCrawl {
    private final String id;
    private final String origin;
    private final LocalDateTime createdAt;
    private final Collection<String> seeds;

    WebsiteCrawl(String id, String origin, LocalDateTime createdAt, Collection<String> seeds) {
        this.id = id;
        this.origin = origin;
        this.createdAt = createdAt;
        this.seeds = seeds;
    }

    public static WebsiteCrawl fromCrawlStartedEvent(String crawlId, CrawlStartedEvent conf) {
        return new WebsiteCrawl(crawlId, conf.getOrigin(), LocalDateTime.now(), conf.getSeeds());
    }
}

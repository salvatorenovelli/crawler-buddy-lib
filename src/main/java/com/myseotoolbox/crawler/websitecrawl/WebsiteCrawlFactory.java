package com.myseotoolbox.crawler.websitecrawl;


import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class WebsiteCrawlFactory {

    public static WebsiteCrawl newWebsiteCrawlFor(String origin, Collection<URI> seeds) {
        return newWebsiteCrawlFor(UUID.randomUUID().toString(), origin, seeds);
    }

    public static WebsiteCrawl newWebsiteCrawlFor(String crawlId, String origin, Collection<URI> seeds) {
        return new WebsiteCrawl(crawlId, origin, LocalDateTime.now(), seeds.stream().map(URI::toString).collect(Collectors.toList()));
    }
}

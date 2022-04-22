package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.websitecrawl.CrawlStartedEvent;
import com.myseotoolbox.crawler.websitecrawl.WebsiteCrawl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class CrawlEventDispatch {

    private final WebsiteCrawl websiteCrawl;


    public void pageCrawled(CrawlResult crawlResult) {
        PageSnapshot snapshot = crawlResult.getPageSnapshot();
        log.debug("Persisting page crawled: {}", snapshot.getUri());

        throw new RuntimeException("Not implemented yet!");
    }

    public void crawlStarted(CrawlStartedEvent event) {
        throw new RuntimeException("Not implemented yet!");
    }

    public void crawlEnded() {
        throw new RuntimeException("Not implemented yet!");
    }
}

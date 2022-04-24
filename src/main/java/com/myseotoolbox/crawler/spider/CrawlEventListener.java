package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.websitecrawl.CrawlStartedEvent;


public interface CrawlEventListener {
    public void pageCrawled(CrawlResult crawlResult);

    public void crawlStarted(CrawlStartedEvent event);

    public void crawlEnded();
}

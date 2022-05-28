package com.myseotoolbox.crawler.utils;

import com.myseotoolbox.crawler.spider.CrawlerThreadPoolExecutorFactory;
import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;

import java.util.concurrent.ThreadPoolExecutor;

public class CurrentCrawlerThreadExecutorFactory extends CrawlerThreadPoolExecutorFactory {
    @Override
    public ThreadPoolExecutor buildThreadPool(String namePostfix, int concurrentConnections) {
        return new CurrentThreadTestExecutorService();
    }
}

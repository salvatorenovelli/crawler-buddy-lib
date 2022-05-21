package com.myseotoolbox.crawler.utils;

import com.myseotoolbox.crawler.spider.ThreadPoolExecutorFactory;
import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;

import java.util.concurrent.ThreadPoolExecutor;

public class CurrentThreadExecutorFactory extends ThreadPoolExecutorFactory {
    @Override
    public ThreadPoolExecutor buildThreadPool(String namePostfix, int concurrentConnections) {
        return new CurrentThreadTestExecutorService();
    }
}

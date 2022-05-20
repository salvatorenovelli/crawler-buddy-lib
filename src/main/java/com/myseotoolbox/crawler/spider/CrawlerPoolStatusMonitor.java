package com.myseotoolbox.crawler.spider;

import lombok.extern.log4j.Log4j2;

import java.lang.ref.WeakReference;
import java.util.concurrent.ThreadPoolExecutor;

@Log4j2
public class CrawlerPoolStatusMonitor {

    private final Thread thread;

    public CrawlerPoolStatusMonitor(String name, ThreadPoolExecutor executor) {
        this.thread = new Thread(() -> {

            WeakReference<ThreadPoolExecutor> reference = new WeakReference<>(executor);
            sleep(1000);
            logStatus(name, executor);
            while (true) {
                sleep(30000);
                ThreadPoolExecutor execRef = reference.get();
                if (execRef == null || execRef.getActiveCount() < 1) {
                    log.info("No active threads. Terminating monitoring for Crawler: {} ({})", name, execRef);
                    break;
                }

                logStatus(name, execRef);

            }
        }, "poolmonitor-" + name);

        thread.setDaemon(true);
    }

    private void logStatus(String name, ThreadPoolExecutor execRef) {
        log.info("{} - {} Pool size: {} Active Threads: {} Queued Tasks: {} Completed Tasks: {}",
                name,
                getRunState(execRef),
                execRef.getPoolSize(),
                execRef.getActiveCount(),
                execRef.getQueue().size(),
                execRef.getCompletedTaskCount());
    }

    private void sleep(int l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException ignore) {
            //ignore
        }
    }

    private String getRunState(ThreadPoolExecutor execRef) {
        return execRef.isTerminated() ? "Terminated" : "Running";
    }

    public void start() {
        thread.start();
    }
}

package com.myseotoolbox.crawler.utils;


import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class LoggingUtils {

    private static final ConcurrentHashMap<String, Long> executedLogs = new ConcurrentHashMap<>();

    public static void logWarningOnce(Object id, Logger logger, String text) {
        executedLogs.computeIfAbsent(getKey(id, text), s -> {
            logger.warn(text);
            return System.currentTimeMillis();
        });
    }

    private static String getKey(Object id, String text) {
        return "" + id.hashCode() + text.hashCode();
    }
}

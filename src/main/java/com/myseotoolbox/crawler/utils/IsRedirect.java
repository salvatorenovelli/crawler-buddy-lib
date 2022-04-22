package com.myseotoolbox.crawler.utils;

import java.net.HttpURLConnection;

public class IsRedirect {
    public static boolean isRedirect(int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_MOVED_PERM: // 301
            case HttpURLConnection.HTTP_MOVED_TEMP: // 302
            case HttpURLConnection.HTTP_SEE_OTHER: // 303
            case 307:
            case 308:
                return true;
            default:
                return false;
        }
    }
}

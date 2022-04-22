package com.myseotoolbox.crawler.spider;


import com.myseotoolbox.crawler.utils.AntPathMatcher;

import static com.myseotoolbox.crawler.spider.configuration.AllowedPathFromSeeds.extractAllowedPathFromSeed;

public class PathMatcher {
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public static boolean isSubPath(String basePath, String possibleSubPath) {

        if (basePath.length() == 0) basePath = "/";
        if (possibleSubPath.length() == 0) possibleSubPath = "/";

        return matcher.match(toAntFilter(extractAllowedPathFromSeed(basePath)), possibleSubPath);
    }

    private static String toAntFilter(String s) {
        return s + "**";
    }

}

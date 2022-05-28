package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.spider.filter.BasicUriFilter;
import com.myseotoolbox.crawler.spider.filter.FilterAggregator;
import com.myseotoolbox.crawler.spider.filter.PathFilter;
import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.List;

@Log4j2
public class DefaultUriFilter implements UriFilter {
    private final FilterAggregator aggregatedFilters;

    public DefaultUriFilter(URI origin, List<String> allowedPaths, RobotsTxt robotsTxt) {
        PathFilter pathFilter = new PathFilter(allowedPaths);
        BasicUriFilter basicFilter = new BasicUriFilter(origin);
        aggregatedFilters = new FilterAggregator(robotsTxt, basicFilter, pathFilter);
    }

    @Override
    public boolean shouldCrawl(URI sourceUri, URI discoveredLink) {
        return aggregatedFilters.shouldCrawl(sourceUri, discoveredLink);
    }
}

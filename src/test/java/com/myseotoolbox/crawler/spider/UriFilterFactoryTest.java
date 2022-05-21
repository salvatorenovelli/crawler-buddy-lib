package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.spider.filter.robotstxt.EmptyRobotsTxt;
import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.myseotoolbox.crawler.spider.configuration.AllowedPathFromSeeds.extractAllowedPathFromSeeds;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UriFilterFactoryTest {

    public static final RobotsTxt DISABLE_ALL_ROBOTS_TXT = new RobotsTxt() {
        @Override
        public List<String> getSitemaps() {
            return null;
        }

        @Override
        public boolean shouldCrawl(URI sourceUri, URI discoveredLink) {
            return false;
        }
    };
    UriFilterFactory sut = new UriFilterFactory();


    @Test
    public void shouldAllowCrawlOutsideAllowedPathIfLinkWasDiscoveredInsideAllowedPath() {

        URI origin = URI.create("http://testhost/subpath/");
        URI allowed = origin.resolve("/allowed/");
        UriFilter build = sut.build(origin, extractAllowedPathFromSeeds(Collections.singletonList(allowed)), new EmptyRobotsTxt(null));

        assertTrue(build.shouldCrawl(allowed, origin.resolve("/salve")));
        assertFalse(build.shouldCrawl(origin.resolve("/outside"), origin.resolve("/salve1")));
    }

    @Test
    public void shouldNotCrawlOtherDomains() {
        URI origin = URI.create("http://testhost/subpath/");
        URI allowed = origin.resolve("/allowed");
        UriFilter build = sut.build(origin, extractAllowedPathFromSeeds(Collections.singletonList(allowed)), new EmptyRobotsTxt(null));

        assertFalse(build.shouldCrawl(allowed, URI.create("http://another-host").resolve("/allowed")));
    }

    @Test
    public void shouldNeverOverrideRobotsTxt() {
        URI origin = URI.create("http://testhost/");
        URI allowed = origin.resolve("/allowed");
        UriFilter build = sut.build(origin, extractAllowedPathFromSeeds(Collections.singletonList(allowed)), DISABLE_ALL_ROBOTS_TXT);

        assertFalse(build.shouldCrawl(allowed, origin.resolve("/allowed")));
    }
}
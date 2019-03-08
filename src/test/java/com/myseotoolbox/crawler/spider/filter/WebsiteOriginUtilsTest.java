package com.myseotoolbox.crawler.spider.filter;

import org.junit.Test;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.*;
import static java.net.URI.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebsiteOriginUtilsTest {


    @Test
    public void verifySubdomain() {
        assertTrue(isSubdomain(create("http://host"), create("http://www.host")));
    }

    @Test
    public void verifyNotSubdomainSimple() {
        assertFalse(isSubdomain(create("http://host"), create("http://something")));
    }

    @Test
    public void verifyNotSubdomainTricky() {
        assertFalse(isSubdomain(create("http://host"), create("http://anotherhost")));
    }

    @Test
    public void isCaseInsensitive() {
        assertTrue(isHostMatching(create("http://host"), create("http://HOST/differentPath")));
    }

    @Test
    public void shouldMatchDifferentPath() {
        assertTrue(isHostMatching(create("http://host"), create("http://host/differentPath")));
    }


    @Test
    public void shouldDistinguishBetweenHostsWithSameStartWith() {
        assertFalse(isHostMatching(create("http://host"), create("http://host2")));
    }

    @Test
    public void isChildOfShouldMatchDifferentPath() {
        assertTrue(isChildOf(create("http://host"), create("http://host/anotherPath")));
    }

    @Test
    public void isChildOfShouldFilterDifferentHosts() {
        assertFalse(isChildOf(create("http://host"), create("http://differentHost/anotherPath")));
    }

    @Test
    public void isChildOfShouldFilterDifferentProtocol() {
        assertFalse(isChildOf(create("https://host"), create("http://host/path")));
    }

    @Test
    public void shouldBeCaseInsensitive() {
        assertTrue(isChildOf(create("http://host"), create("http://HOST/path")));
    }
}
package com.myseotoolbox.crawler.spider.sitemap;

import com.myseotoolbox.crawler.spider.UriFilter;
import com.myseotoolbox.crawler.spider.filter.PathFilter;
import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class SitemapReaderTest {

    private TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();
    private SitemapReader sut = new SitemapReader();

    @Before
    public void setUp() throws Exception {
        testWebsiteBuilder.run();
    }

    @After
    public void tearDown() throws Exception {
        testWebsiteBuilder.tearDown();
    }


    @Test
    public void shouldGetSeedsFromSitemapsIncludingFiltering() {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2", "/outside/shouldnotaddthis")
                .build();

        List<URI> uris = sut.getSeedsFromSitemaps(testUri("/"), testUris("/sitemap.xml"), allowingPath("/"));

        assertThat(uris, hasItems(testUri("/location1"), testUri("/location2")));
    }

    @Test
    public void shouldOnlyReturnValidUri() {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2", "/should not add this")
                .build();

        List<URI> uris = sut.getSeedsFromSitemaps(testUri("/"), testUris("/sitemap.xml"), allowingPath("/"));

        assertThat(uris, hasItems(testUri("/location1"), testUri("/location2")));
    }

    @Test
    public void shouldOnlyGetSameDomain() {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2", "http://another-domain/")
                .build();

        List<URI> uris = sut.getSeedsFromSitemaps(testUri("/"), testUris("/sitemap.xml"), allowingPath("/"));

        assertThat(uris, hasItems(testUri("/location1"), testUri("/location2")));
    }

    private URI testUri(String s) {
        return testWebsiteBuilder.buildTestUri(s);
    }

    private TestWebsiteBuilder givenAWebsite() {
        return testWebsiteBuilder;
    }

    private List<String> testUris(String url) {
        return Collections.singletonList(testWebsiteBuilder.buildTestUri(url).toString());
    }

    private UriFilter allowingPath(String path) {
        return new PathFilter(Collections.singletonList(path));
    }
}
package com.myseotoolbox.crawler.spider.sitemap;

import com.myseotoolbox.crawler.testutils.TestWebsite;
import com.myseotoolbox.crawler.testutils.testwebsite.ReceivedRequest;
import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class SiteMapTest {

    public static final List<String> ALLOW_ALL = Collections.singletonList("/");
    private TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();
    private TestWebsite testWebsite;

    private URI origin;
    SiteMap siteMap;

    @Before
    public void setUp() throws Exception {
        testWebsite = testWebsiteBuilder.run();
        origin = testUri("/");
        siteMap = new SiteMap(origin, uri("/sitemap.xml"));
    }

    @After
    public void tearDown() throws Exception {
        testWebsiteBuilder.tearDown();
    }

    @Test
    public void shouldAllowWWWButNotOtherSubdomains() throws UnknownHostException {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls(testUri("api", "/locationApi").toString(), testUri("www", "/locationWww").toString(), "/location2")
                .build();

        List<String> urls = siteMap.fetchUris();

        assertThat(urls, containsInAnyOrder(uri("www", "/locationWww"), uri("/location2")));
    }

    @Test
    public void canGetUrisFromSimpleSiteMap() throws IOException {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2")
                .build();

        List<String> urls = siteMap.fetchUris();


        assertThat(urls, hasItems(uri("/location1"), uri("/location2")));
    }

    @Test
    public void shouldDeduplicateUrls() throws MalformedURLException {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2", "/location2")
                .build();

        List<String> urls = siteMap.fetchUris();

        assertThat(urls, hasSize(2));
        assertThat(urls, hasItems(uri("/location1"), uri("/location2")));
    }

    @Test
    public void canRecursivelyResolveSiteMapUrls() throws MalformedURLException {
        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/it/", "/uk/")
                .and()
                .withSitemapOn("/it/")
                .havingUrls("/it/1", "/it/2")
                .and()
                .withSitemapOn("/uk/")
                .havingUrls("/uk/1", "/uk/2").build();

        List<String> urls = siteMap.fetchUris();

        assertThat(urls, hasItems(uri("/it/1"), uri("/it/2"), uri("/uk/1"), uri("/uk/2")));
    }


    @Test
    public void shouldNotGetUrlsFromFilteredSitemap() throws MalformedURLException {
        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/it/", "/uk/")
                .and()
                .withSitemapOn("/it/")
                .havingUrls("/it/1", "/it/2")
                .and()
                .withSitemapOn("/uk/")
                .havingUrls("/uk/1", "/uk/2").build();


        SiteMap siteMap = new SiteMap(origin, uris("/sitemap.xml"), Collections.singletonList("/it/"));
        List<String> urls = siteMap.fetchUris();

        assertThat(urls, hasSize(2));
        assertThat(urls, hasItems(uri("/it/1"), uri("/it/2")));

    }

    @Test
    public void shouldBeTolerantToTrailingSlashInAllowedPaths() throws MalformedURLException {
        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/it/", "/uk/")
                .and()
                .withSitemapOn("/it/")
                .havingUrls("/it/1", "/it/2")
                .and()
                .withSitemapOn("/uk/")
                .havingUrls("/uk/1", "/uk/2").build();


        SiteMap siteMap = new SiteMap(origin, uris("/sitemap.xml"), Collections.singletonList("/it/"));
        List<String> urls = siteMap.fetchUris();

        assertThat(urls, hasSize(2));
        assertThat(urls, hasItems(uri("/it/1"), uri("/it/2")));
    }

    @Test
    public void shouldNotFetchUnnecessarySitemaps() {
        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/it/", "/uk/")
                .and()
                .withSitemapOn("/it/")
                .havingUrls("/it/1", "/it/2", "/uk/")
                .and()
                .withSitemapOn("/uk/")
                .havingUrls("/uk/1", "/uk/2").build();

        SiteMap siteMap = new SiteMap(origin.resolve("/it/"), uris("/sitemap.xml"), Collections.singletonList("/it/"));
        siteMap.fetchUris();

        List<String> requestsReceived = testWebsite.getRequestsReceived().stream().map(ReceivedRequest::getUrl).collect(toList());
        assertThat(requestsReceived, hasSize(2));
        assertThat(requestsReceived, hasItems("/sitemap.xml", "/it/sitemap.xml"));
    }

    @Test
    public void shouldNotListUrlWithDifferentDomains() {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/location1", "/location2", "https://differentdomain/location2")
                .build();

        List<String> uris = siteMap.fetchUris();

        assertThat(uris, hasSize(2));
        assertThat(uris, hasItems(uri("/location1"), uri("/location2")));
    }


    @Test
    public void shouldNotFetchSitemapsOnDifferentDomains() throws Exception {

        TestWebsiteBuilder differentDomain = TestWebsiteBuilder.build();
        TestWebsite differentDomainWebsite = differentDomain.run();


        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/it/", differentDomain.buildTestUri("/differentDomain/").toString())
                .and()
                .withSitemapOn("/it/")
                .havingUrls("/it/1", "/it/2").build();


        siteMap.fetchUris();

        assertThat(differentDomainWebsite.getRequestsReceived(), hasSize(0));
    }


    @Test
    public void shouldFetchMultipleSitemaps() {
        givenAWebsite()
                .withSitemapOn("/one/")
                .havingUrls("/one/1", "/one/2")
                .and()
                .withSitemapOn("/two/")
                .havingUrls("/two/1", "/two/2").build();

        SiteMap siteMap = new SiteMap(origin, uris("/one/sitemap.xml", "/two/sitemap.xml"), Collections.singletonList("/"));
        List<String> uris = siteMap.fetchUris();


        assertThat(uris, hasSize(4));
        assertThat(uris, hasItems(uri("/one/1"), uri("/one/2"), uri("/two/1"), uri("/two/2")));

    }

    @Test
    public void shouldDedupWhenFetchingMultipleSitemaps() {
        givenAWebsite()
                .withSitemapOn("/sitemap_one.xml")
                .havingUrls("/1", "/2")
                .and()
                .withSitemapOn("/sitemap_two.xml")
                .havingUrls("/1", "/3").build();

        SiteMap siteMap = new SiteMap(origin, uris("/sitemap_one.xml", "/sitemap_two.xml"), Collections.singletonList("/"));
        List<String> uris = siteMap.fetchUris();


        assertThat(uris, hasSize(3));
        assertThat(uris, hasItems(uri("/1"), uri("/2"), uri("/3")));
    }

    @Test
    public void shouldBeAbleToDealWithMalformedUriInSitemapIndex() {
        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/invalid url/")
                .havingChildSitemaps("/valid-url/").and()
                .withSitemapOn("/valid-url/")
                .havingUrls("/valid-url/1", "/valid-url/2")
                .build();

        List<String> uris = siteMap.fetchUris();


        assertThat(uris, hasSize(2));
        assertThat(uris, hasItems(uri("/valid-url/1"), uri("/valid-url/2")));
    }

    @Test
    public void shouldBeAbleToDealWithMalformedUriInSitemap() {
        givenAWebsite()
                .withSitemapOn("/")
                .havingUrls("/valid-url1", "/valid-url2", "/invalid url")
                .build();

        List<String> uris = siteMap.fetchUris();


        assertTrue(uris.size() >= 2);
    }


    @Test
    public void shouldNotFetchSitemapOutsideOrigin() throws Exception {
        givenAWebsite().withSitemapOn("/").havingUrls("/correct-domain-url").build();

        TestWebsiteBuilder wrongWebsiteBuilder = TestWebsiteBuilder.build();
        TestWebsite wrongWebsite = wrongWebsiteBuilder.withSitemapOn("/").havingUrls("/wrong-domain-url").build().run();


        SiteMap sut = new SiteMap(testUri("/"), Collections.singletonList(wrongWebsiteBuilder.buildTestUri("/sitemap.xml").toString()), Collections.singletonList("/"));
        List<String> uris = sut.fetchUris();


        assertThat(wrongWebsite.getRequestsReceived().size(), is(0));
        assertThat(uris, hasSize(0));
    }


    @Test
    public void shouldNotFetchSitemapOutsideOriginWhenLinkedFromSitemapIndex() throws Exception {

        TestWebsiteBuilder wrongWebsiteBuilder = TestWebsiteBuilder.build();
        TestWebsite wrongWebsite = wrongWebsiteBuilder.withSitemapOn("/").havingUrls("/wrong-domain-url").build().run();

        givenAWebsite()
                .withSitemapIndexOn("/")
                .havingChildSitemaps("/correct/", wrongWebsiteBuilder.buildTestUri("/wrong-sitemap.xml").toString()).and()
                .withSitemapOn("/correct/sitemap.xml").havingUrls("/correct/correct-domain-url").build();


        SiteMap sut = new SiteMap(testUri("/"), Collections.singletonList(testUri("/sitemap.xml").toString()), Collections.singletonList("/"));
        List<String> uris = sut.fetchUris();

        assertThat(wrongWebsite.getRequestsReceived().size(), is(0));
        assertThat(uris, hasItems(testUri("/correct/correct-domain-url").toString()));
    }

    private String uri(String s) {
        return testUri(s).toString();
    }

    private String uri(String subDomain, String path) {
        return testUri(subDomain, path).toString();
    }

    private List<String> uris(String... s) {
        return Arrays.stream(s).map(this::testUri).map(URI::toString).collect(toList());
    }


    private URI testUri(String url) {
        return testWebsiteBuilder.buildTestUri("", url);
    }

    private URI testUri(String subDomain, String url) {
        return testWebsiteBuilder.buildTestUri(subDomain, url);
    }

    private TestWebsiteBuilder givenAWebsite() {
        return testWebsiteBuilder;
    }

}
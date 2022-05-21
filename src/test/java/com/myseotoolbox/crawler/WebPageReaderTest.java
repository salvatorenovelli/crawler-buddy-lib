package com.myseotoolbox.crawler;

import com.myseotoolbox.crawler.httpclient.HttpRequestFactory;
import com.myseotoolbox.crawler.httpclient.NoSSLVerificationConnectionFactory;
import com.myseotoolbox.crawler.httpclient.SnapshotException;
import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.pagelinks.PageLink;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.model.RedirectChainElement;
import com.myseotoolbox.crawler.spider.UriFilter;
import com.myseotoolbox.crawler.testutils.TestWebsite;
import com.myseotoolbox.crawler.testutils.testwebsite.ReceivedRequest;
import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.myseotoolbox.crawler.httpclient.HttpGetRequest.BOT_NAME;
import static com.myseotoolbox.crawler.httpclient.HttpGetRequest.BOT_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


@SuppressWarnings("unchecked")
public class WebPageReaderTest {

    public static final String TEST_TITLE = "Test withTitle";
    public static final String TEST_ROOT_PAGE_PATH = "/";
    public static final String TEST_REDIRECT_URL = "/another_url";
    public static final UriFilter ALLOW_ALL_URI = (sourceUri, discoveredLink) -> true;


    private WebPageReader sut;
    private TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();
    private HttpRequestFactory httpRequestFactory = new HttpRequestFactory(new NoSSLVerificationConnectionFactory());

    @Before
    public void setUp() {
        sut = new WebPageReader(ALLOW_ALL_URI, httpRequestFactory);
    }

    @After
    public void tearDown() throws Exception {
        testWebsiteBuilder.tearDown();
    }

    @Test
    public void dateShouldBeSet() throws Exception {
        givenAWebsite().havingRootPage()
                .run();

        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();
        assertNotNull(snapshot.getCreateDate());
    }

    @Test
    public void shouldRetrieveTitle() throws Exception {
        givenAWebsite().havingRootPage()
                .withTitle(TEST_TITLE)
                .run();
        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();
        assertThat(snapshot.getTitle(), is(TEST_TITLE));
    }

    @Test
    public void shouldReadTags() throws Exception {
        givenAWebsite().havingRootPage()
                .withTag("H1", "Test H1")
                .run();
        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        List<String> h1s = snapshot.getH1s();

        assertThat(h1s, hasSize(1));
        assertThat(h1s.get(0), is("Test H1"));
    }

    @Test
    public void shouldReadMultipleTags() throws Exception {
        givenAWebsite().havingRootPage()
                .withTag("H1", "Test H1")
                .withTag("H1", "Test second H1")
                .run();
        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        List<String> h1s = snapshot.getH1s();

        assertThat(h1s, hasSize(2));
        assertThat(h1s.get(0), is("Test H1"));
        assertThat(h1s.get(1), is("Test second H1"));
    }

    @Test
    public void shouldReadMultipleDiverseTags() throws Exception {
        givenAWebsite().havingRootPage()
                .withTag("H1", "Test H1")
                .withTag("H1", "Test second H1")
                .withTag("H2", "Test H2")
                .run();

        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        List<String> h1s = snapshot.getH1s();
        List<String> h2s = snapshot.getH2s();

        assertThat(h1s, hasSize(2));
        assertThat(h2s, hasSize(1));

        assertThat(h2s.get(0), is("Test H2"));

    }

    @Test
    public void shouldDiscoverLinks() throws Exception {
        givenAWebsite().havingRootPage()
                .withLinksTo("/dst1", "/dst2")
                .run();

        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        assertThat(snapshot.getLinks(), hasSize(2));
        assertThat(snapshot.getLinks().get(0).getDestination(), is("/dst1"));
        assertThat(snapshot.getLinks().get(1).getDestination(), is("/dst2"));
    }

    @Test
    public void shouldMapLinkAttributes() throws Exception {
        givenAWebsite().havingRootPage()
                .withNoFollowLinksTo("/dst")
                .run();

        PageSnapshot snapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        assertThat(snapshot.getLinks(), hasSize(1));
        PageLink link0 = snapshot.getLinks().get(0);

        Set<Map.Entry<String, String>> attributes = link0.getAttributes().entrySet();
        assertThat(attributes, hasSize(1));
        Map.Entry<String, String> attribute = attributes.iterator().next();
        assertThat(attribute.getKey(), is("rel"));
        assertThat(attribute.getValue(), is("nofollow"));
    }

    @Test
    public void destinationUrlShouldBeSavedAsAbsolute() throws Exception {

        givenAWebsite()
                .havingPage(TEST_ROOT_PAGE_PATH).redirectingTo(301, "/dst").and()
                .havingPage("/dst").withTitle("You've reached the right place!")
                .run();

        PageSnapshot pageSnapshot = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        assertThat(getDestinationUri(pageSnapshot), is(testUri("/dst").toString()));
        assertThat(pageSnapshot.getTitle(), is("You've reached the right place!"));
    }


    @Test
    public void redirectChainIsSaved() throws Exception {

        givenAWebsite()
                .havingPage("/").redirectingTo(301, "/dst1").and()
                .havingPage("/dst1").redirectingTo(302, "/dst2").and()
                .havingPage("/dst2").redirectingTo(301, "/dst3").and()
                .havingPage("/dst3").withTitle("You've reached the right place!")
                .run();

        PageSnapshot pageSnapshot = sut.snapshotPage(testUri("/")).getPageSnapshot();

        assertThat(pageSnapshot.getRedirectChainElements(), contains(
                el(301, "/dst1"),
                el(302, "/dst2"),
                el(301, "/dst3"),
                el(200, "/dst3")));

    }


    @Test
    public void pageNotFound404ShouldBeHandled() throws Exception {

        givenAWebsite()
                .havingPage("/page").redirectingTo(301, "/dst1")
                .run();

        PageSnapshot pageSnapshot = sut.snapshotPage(testUri("/page")).getPageSnapshot();

        assertThat(pageSnapshot.getRedirectChainElements(), contains(
                el(301, "/dst1"),
                el(404, "/dst1")));


    }

    @Test
    public void redirectLoopShouldBeHandledGracefully() throws Exception {
        givenAWebsite()
                .havingPage("/start").redirectingTo(301, "/dst1").and()
                .havingPage("/dst1").redirectingTo(301, "/dst2").and()
                .havingPage("/dst2").redirectingTo(301, "/start")
                .run();

        try {
            sut.snapshotPage(testUri("/start"));
        } catch (SnapshotException e) {

            assertThat(e.getPartialSnapshot().getRedirectChainElements(), contains(
                    el(301, "/dst1"),
                    el(301, "/dst2"),
                    el(301, "/start")));

            assertThat(e.getPartialSnapshot().getCrawlStatus(), containsString("ERROR: Redirect Loop"));
            return;
        }

        fail("Expected exception");
    }

    @Test
    public void shouldHaveAPoliteUserAgent() throws Exception {

        TestWebsite website = givenAWebsite().havingRootPage()
                .withTitle(TEST_TITLE)
                .run();

        sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        List<ReceivedRequest> requests = website.getRequestsReceived();

        assertThat(requests.get(0).getUserAgent(), is("Mozilla/5.0 (compatible; " + BOT_NAME + "/" + BOT_VERSION + ")"));

    }


    @Test
    public void shouldPerformOneRequestWhenNoRedirectIsRequired() throws Exception {
        TestWebsite website = givenAWebsite().havingRootPage()
                .withTitle(TEST_TITLE)
                .run();

        sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH)).getPageSnapshot();

        assertThat(website.getRequestsReceived().size(), is(1));

    }


    @Test
    public void shouldPerformOneRequestPerRedirect() throws Exception {
        TestWebsite website = givenAWebsite()
                .havingPage("/start").redirectingTo(301, "/dst1").and()
                .havingPage("/dst1").redirectingTo(301, "/dst2").and()
                .havingPage("/dst2").redirectingTo(301, "/start")
                .run();

        try {
            sut.snapshotPage(testUri("/start"));
            fail("Expected Exception");
        } catch (SnapshotException e) {
            //We expect the exception
        }

        assertThat(website.getRequestsReceived().size(), is(3));
    }

    @Test
    public void itShouldOnlyConsumeSupportedMimeType() throws Exception {
        //content-type should be text/*

        givenAWebsite()
                .havingRootPage().withMimeType("application/pdf")
                .run();

        try {
            sut.snapshotPage(testUri("/"));
        } catch (SnapshotException e) {
            assertThat(e.getPartialSnapshot().getCrawlStatus(), containsString("Unhandled content type"));
            return;
        }

        fail("Expected exception");

    }

    @Test
    public void redirectChainShouldBePresentInCaseOfMimeException() throws Exception {
        //content-type should be text/*
        givenAWebsite()
                .havingRootPage().withMimeType("application/pdf")
                .run();
        try {
            sut.snapshotPage(testUri("/"));
        } catch (SnapshotException e) {
            assertThat(e.getPartialSnapshot().getRedirectChainElements(), hasSize(1));
            assertThat(e.getPartialSnapshot().getRedirectChainElements().get(0).getHttpStatus(), is(200));
            return;
        }

        fail("Expected exception");
    }

    @Test
    public void inCaseOfExceptionShouldReturnAppropriateDefaults() {
        try {
            sut.snapshotPage(URI.create("/"));
            fail("Expected exception");
        } catch (SnapshotException e) {
            PageSnapshot defaultVal = e.getPartialSnapshot();
            assertThat(defaultVal.getUri(), is("/"));
            assertNotNull(defaultVal.getCreateDate());
            assertNotNull(defaultVal.getRedirectChainElements());
            assertThat(defaultVal.getCrawlStatus(), containsString("Unable to crawl:"));
        }
    }


    @Test
    public void shouldReturnBlockedSnapshotIfUrlIsDisallowed() throws Exception {

        sut = new WebPageReader((sourceUri, discoveredLink) -> !discoveredLink.toString().endsWith("/disallowed"), httpRequestFactory);

        givenAWebsite()
                .havingPage(TEST_ROOT_PAGE_PATH).redirectingTo(301, "/allowed").and()
                .havingPage("/allowed").redirectingTo(301, "/disallowed").and()
                .havingPage("/disallowed").withTitle("Whoa! You are not supposed to be here!")
                .run();

        CrawlResult crawlResult = sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH));

        assertTrue(crawlResult.isBlockedChain());
        assertNull(crawlResult.getPageSnapshot());

        List<RedirectChainElement> chainElements = crawlResult.getChain().getElements();
        assertThat(chainElements, hasSize(2));
        assertThat(chainElements.get(0).getDestinationURI(), is(testUri("/allowed").toString()));
        assertThat(chainElements.get(1).getDestinationURI(), is(testUri("/disallowed").toString()));

    }

    @Test
    public void shouldNotFetchDisallowedUri() throws Exception {

        sut = new WebPageReader((sourceUri, discoveredLink) -> !discoveredLink.toString().endsWith("/disallowed"), httpRequestFactory);

        TestWebsite website = givenAWebsite()
                .havingPage(TEST_ROOT_PAGE_PATH).redirectingTo(301, "/allowed").and()
                .havingPage("/allowed").redirectingTo(301, "/disallowed").and()
                .havingPage("/disallowed").withTitle("Whoa! You are not supposed to be here!")
                .run();

        sut.snapshotPage(testUri(TEST_ROOT_PAGE_PATH));

        List<ReceivedRequest> requests = website.getRequestsReceived();

        assertThat(requests, hasSize(2));
        assertThat(requests.get(0).getUrl(), is(TEST_ROOT_PAGE_PATH));
        assertThat(requests.get(1).getUrl(), is(TEST_ROOT_PAGE_PATH + "allowed"));

    }

    @Test
    public void redirectDestinationShouldBeAValidUri() throws Exception {
        givenAWebsite()
                .havingPage("/link withspaces").withTitle("You've reached the right place!")
                .run();

        PageSnapshot pageSnapshot = sut.snapshotPage(testUri("/link withspaces")).getPageSnapshot();

        assertThat(getDestinationUri(pageSnapshot), is(testUri("/link%20withspaces").toString()));
    }

    private String getDestinationUri(PageSnapshot pageSnapshot) {
        List<RedirectChainElement> redirectChainElements = pageSnapshot.getRedirectChainElements();
        return redirectChainElements.get(redirectChainElements.size() - 1).getDestinationURI();
    }

    private URI testUri(String s) {
        return testWebsiteBuilder.buildTestUri(s);
    }

    private TestWebsiteBuilder givenAWebsite() {
        return testWebsiteBuilder;
    }

    private Matcher<RedirectChainElement> el(int status, String dstUri) {

        String uri = testWebsiteBuilder.buildTestUri(dstUri).toString();

        return new BaseMatcher<RedirectChainElement>() {
            @Override
            public boolean matches(Object item) {
                RedirectChainElement element = (RedirectChainElement) item;
                return element.getHttpStatus() == status && element.getDestinationURI().equals(uri);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("{ " + status + ", '" + uri + "' }");
            }
        };
    }
}
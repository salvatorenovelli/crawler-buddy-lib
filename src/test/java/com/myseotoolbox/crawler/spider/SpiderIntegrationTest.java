package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.testutils.TestWebsite;
import com.myseotoolbox.crawler.testutils.testwebsite.ReceivedRequest;
import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
import com.myseotoolbox.crawler.utils.CurrentThreadExecutorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.extractOrigin;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class SpiderIntegrationTest {

    private ThreadPoolExecutorFactory currentThreadThreadPoolFactory = new CurrentThreadExecutorFactory();

    @Mock
    private CrawlEventListener dispatch;


    TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();

    @Before
    public void setUp() throws Exception {
        testWebsiteBuilder.run();
    }

    @After
    public void tearDown() throws Exception {
        testWebsiteBuilder.tearDown();
    }

    @Test
    public void basicLinkFollowing() {

        givenAWebsite()
                .havingPage("/").withLinksTo("/abc", "/cde")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/abc"));
        verify(dispatch).pageCrawled(uri("/cde"));

        verify(dispatch, atMost(3)).pageCrawled(any());

    }

    @Test
    public void urlsWithFragmentsShouldBeNormalized() {
        givenAWebsite()
                .havingPage("/").withLinksTo("/another-page", "/another-page#reviews")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/another-page"));
        verify(dispatch, atMost(2)).pageCrawled(any());
    }

    @Test
    public void websiteWithoutRobotsCanHaveSitemap() {
        givenAWebsite()
                .withSitemapOn("/").havingUrls("/link1", "/link2").build()
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/link1"));
        verify(dispatch).pageCrawled(uri("/link2"));
    }

    @Test
    public void sitemapUrlsWithFragmentShouldBeNormalized() {
        givenAWebsite()
                .withSitemapOn("/").havingUrls("/another-page", "/another-page#reviews").and()
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/another-page"));
        verify(dispatch, atMost(2)).pageCrawled(any());
    }

    @Test
    public void shouldOnlyFilterFromSpecifiedPaths() {
        givenAWebsite()
                .havingPage("/base/").withLinksTo("/base/abc", "/base/cde", "/outside/fgh").and()
                .havingPage("/outside/fgh").withLinksTo("/outside/1234")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/base/"));
        job.start();


        verify(dispatch).pageCrawled(uri("/base/"));
        verify(dispatch).pageCrawled(uri("/base/abc"));
        verify(dispatch).pageCrawled(uri("/base/cde"));
        verify(dispatch).pageCrawled(uri("/outside/fgh"));

        verify(dispatch, atMost(4)).pageCrawled(any());
    }


    @Test
    public void shouldCrawlOutsideSeedsIfComingFromInside() {
        givenAWebsite()
                .havingPage("/base/").withLinksTo("/outside", "/").and()
                .havingPage("/outside").withLinksTo("/outside/1234")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/base/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/base/"));
        verify(dispatch).pageCrawled(uri("/outside"));

        verify(dispatch, atMost(3)).pageCrawled(any());
    }

    @Test
    public void multipleSeedsActAsFilters() {


        givenAWebsite()
                .havingPage("/base/").withLinksTo("/base/abc", "/base/cde", "/base2/fgh", "/outside/a").and()
                .havingPage("/outside/a").withLinksTo("/outside/b")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/base/", "/base2/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/base/"));
        verify(dispatch).pageCrawled(uri("/base2/"));
        verify(dispatch).pageCrawled(uri("/base/abc"));
        verify(dispatch).pageCrawled(uri("/base/cde"));
        verify(dispatch).pageCrawled(uri("/base2/fgh"));
        verify(dispatch).pageCrawled(uri("/outside/a"));

        verify(dispatch, atMost(6)).pageCrawled(any());


    }

    @Test
    public void shouldNotVisitOtherDomains() {

        givenAWebsite()
                .havingPage("/base").withLinksTo("/base/abc", "http://differentdomain")
                .save();

        CrawlJob job = buildForSeeds(testSeeds("/base", "/base2"));
        job.start();

        verify(dispatch).pageCrawled(uri("/base"));
        verify(dispatch).pageCrawled(uri("/base2"));
        verify(dispatch).pageCrawled(uri("/base/abc"));

        verify(dispatch, atMost(3)).pageCrawled(any());
    }

    @Test
    public void shouldNotVisitBlockedUriInRedirectChain() {
        TestWebsite testWebsite = givenAWebsite()
                .withRobotsTxt().userAgent("*").disallow("/blocked-by-robots").build()
                .havingRootPage().redirectingTo(301, "/blocked-by-robots").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        assertThat(getReceivedRequests(testWebsite), not(hasItems("/blocked-by-robots")));
    }

    @Test
    public void shouldNotNotifyListenersWhenChainIsBlocked() {
        givenAWebsite()
                .withRobotsTxt().userAgent("*").disallow("/blocked-by-robots").build()
                .havingRootPage().withLinksTo("/dst1", "dst2").and()
                .havingPage("/dst2").redirectingTo(301, "/blocked-by-robots").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/dst1"));
        verify(dispatch, atMost(2)).pageCrawled(any());
    }


    @Test
    public void shouldSanitizeTags() {
        givenAWebsite().havingRootPage().withTitle("This <b>has</b> leading spaces    ").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(argThat(snapshot -> {
            assertThat(snapshot.getPageSnapshot().getTitle(), is("This has leading spaces"));
            return true;
        }));
    }

    @Test
    public void shouldTrimUrls() {
        givenAWebsite()
                .havingRootPage().withLinksTo("/dst1   ").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/dst1"));
        verify(dispatch, atMost(2)).pageCrawled(any());
    }

    @Test
    public void shouldNotVisitMoreThanRequired() {
        TestWebsite testWebsite = givenAWebsite()
                .withRobotsTxt().userAgent("*").disallow("/disallowed").build()
                .havingRootPage().withLinksTo("/disallowed").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        assertThat(getReceivedRequests(testWebsite), containsInAnyOrder("/", "/robots.txt"));

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch, atMost(1)).pageCrawled(any());

    }

    @Test
    public void shouldNotVisitPagesBlockedInRobotsTxt() {
        TestWebsite testWebsite = givenAWebsite()
                .withRobotsTxt().userAgent("*").disallow("/disallowed").build()
                .havingRootPage().withLinksTo("/disallowed").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        assertThat(getReceivedRequests(testWebsite), not(hasItems("/disallowed")));
        verify(dispatch, never()).pageCrawled(uri("/disallowed"));

    }

    @Test
    public void websiteWithRedirectDestinationWithSpacesShouldResolveLinksProperly() {
        givenAWebsite()
                .havingRootPage().redirectingTo(301, "/link withspaces/base").and()
                .havingPage("/link withspaces/base").withLinksTo("relative").save();

        CrawlJob job = buildForSeeds(testSeeds("/"));
        job.start();

        verify(dispatch).pageCrawled(uri("/"));
        verify(dispatch).pageCrawled(uri("/link%20withspaces/relative"));
        verify(dispatch, atMost(2)).pageCrawled(any());
    }


    private CrawlResult uri(String uri) {
        return argThat(argument -> argument.getPageSnapshot().getUri().equals(testUri(uri).toString()));
    }

    private CrawlJob buildForSeeds(List<URI> seeds){

        URI origin = extractOrigin(seeds.get(0));

        return CrawlJobBuilder
                .newCrawlJobFor(origin, dispatch)
                .withSeeds(seeds)
                .withConcurrentConnections(seeds.size())
                .withThreadPoolFactory(currentThreadThreadPoolFactory)
                .build();
    }


    private URI testUri(String url) {
        return testWebsiteBuilder.buildTestUri(url);
    }

    private List<URI> testSeeds(String... urls) {
        return Arrays.stream(urls).map(s -> testWebsiteBuilder.buildTestUri(s)).collect(Collectors.toList());
    }

    private TestWebsiteBuilder givenAWebsite() {
        return testWebsiteBuilder;
    }

    private List<String> getReceivedRequests(TestWebsite testWebsite) {
        return testWebsite.getRequestsReceived().stream().map(ReceivedRequest::getUrl).collect(Collectors.toList());
    }

}

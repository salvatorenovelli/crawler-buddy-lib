package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.model.*;
import com.myseotoolbox.crawler.pagelinks.PageLink;
import com.myseotoolbox.crawler.spider.model.SnapshotTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.myseotoolbox.crawler.spider.PageLinksHelper.MAX_URL_LEN;
import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CrawlerQueueTest {

    private static final UriFilter NO_URI_FILTER = (s, d) -> true;
    private static final int MAX_CRAWLS = 100;
    private static final String LOCATION_WITH_UNICODE_CHARACTERS = "/família";
    private static final String QUEUE_NAME = "name";


    private CrawlerQueue sut;
    @Mock private CrawlersPool pool;
    @Mock private CrawlEventListener dispatch;

    @Before
    public void setUp() {
        doAnswer(invocation -> {
            SnapshotTask task = invocation.getArgument(0);
            task.getTaskRequester().accept(CrawlResult.forSnapshot(aPageSnapshotWithStandardValuesForUri(task.getUri().toString())));
            return null;
        }).when(pool).accept(any());
        sut = initSut().build();
    }


    @Test
    public void shouldRequireProcessingOfAllSeeds() {
        sut.start();
        verify(pool).accept(taskForUri("http://host1"));
    }

    @Test
    public void shouldNotVisitTheSameUrlTwice() {
        whenCrawling("http://host1").discover("http://host1");
        sut.start();
        verify(pool).accept(taskForUri("http://host1"));
    }

    @Test
    public void shouldNotifyListenersWithSnapshotResult() {
        whenCrawling("http://host1").discover("http://host1/dst");
        sut.start();
        verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://host1")));
    }

    @Test
    public void shouldNotVisitNoFollow() {
        whenCrawling("http://host1").discover(Collections.singletonList(new PageLink("http://host1/dst", singletonMap("rel", "nofollow"))));
        sut.start();
        verify(pool).accept(taskForUri("http://host1"));
        verify(pool,atMost(1)).accept(any());
    }

    @Test
    public void shouldNotifyListenerWithResultHavingUnicodeUri() {
        whenCrawling("http://host1").discover("http://host1/linkWithUnicode\u200B  \u200B");
        sut.start();
        verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://host1")));
        verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://host1/linkWithUnicode%E2%80%8B%20%20%E2%80%8B")));
    }

    @Test
    public void shouldEncodeUnicodeCharsBeforeCrawling() {
        whenCrawling("http://host1").discover("http://host1/linkWithUnicode\u200B  \u200B");
        sut.start();
        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/linkWithUnicode%E2%80%8B%20%20%E2%80%8B"));
    }

    @Test
    public void shouldNotVisitTwiceIfRelativeWasVisitedAndAbsoluteIsDiscovered() {
        whenCrawling("http://host1").discover("/dst");
        whenCrawling("http://host1/dst").discover("http://host1/dst");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst"));
    }

    @Test
    public void shouldVisitTwiceIfDifferentVersionOfRootIsDiscovered() {
        whenCrawling("http://host1").discover("/");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/"));
    }


    @Test
    public void takeAlwaysReturnAbsoluteUri() {
        whenCrawling("http://host1").discover("/dst");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst"));
    }


    @Test
    public void onSnapshotCompleteShouldNeverBeFedRelativeUrisAsTakeNeverReturnsIt() {
        sut = initSut().withUris("http://host1/dst").build();

        try {
            sut.accept(CrawlResult.forSnapshot(aPageSnapshotWithStandardValuesForUri("/dst")));
        } catch (IllegalStateException e) {
            //success!!
            assertThat(e.getMessage(), containsString("URI should be absolute"));
            return;
        }

        fail("Expected exception");
    }

    @Test
    public void completingASnapshotNeverSubmittedShouldThrowException() {

        sut = initSut().withUris("http://host1/dst").build();

        try {
            sut.accept(CrawlResult.forSnapshot(aPageSnapshotWithStandardValuesForUri(("http://host1/dst"))));
        } catch (IllegalStateException e) {
            //success!!
            assertThat(e.getMessage(), containsString("Completing snapshot of not in progress URI: 'http://host1/dst' Visited: false"));
            return;
        }

        fail("Expected exception");
    }

    @Test
    public void shouldAlertIfCompleteTheSameSnapshotTwice() {

        sut.start();

        try {
            sut.accept(CrawlResult.forSnapshot(aPageSnapshotWithStandardValuesForUri(("http://host1"))));
        } catch (IllegalStateException e) {
            //success!!
            assertThat(e.getMessage(), containsString("Completing snapshot of not in progress URI: 'http://host1' Visited: true"));
            return;
        }

        fail("Expected exception");
    }

    @Test
    public void shouldProcessAllSeedsWhenMultipleSeedsAreFed() {

        sut = initSut().withUris("http://host1", "http://host2").build();
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host2"));

    }


    @Test
    public void canResolveLinksWithUnicodeChars() {
        whenCrawling("http://host1").discover(LOCATION_WITH_UNICODE_CHARACTERS);

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1" + "/fam%C3%ADlia"));
    }

    @Test
    public void discoveringDuplicateLinksInPageDoesNotEnqueueItMultipleTimes() {
        whenCrawling("http://host1").discover("http://host1/dst1", "http://host1/dst2", "http://host1/dst1");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst1"));
        verify(pool).accept(taskForUri("http://host1/dst2"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldNotAcceptFilteredUris() {
        whenCrawling("http://host1").discover("http://host1/dst1", "http://host1/dst2", "http://host1/shouldReject");

        sut = initSut().withFilter((s, d) -> !d.toString().equals("http://host1/shouldReject")).build();
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst1"));
        verify(pool).accept(taskForUri("http://host1/dst2"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }


    @Test
    public void submittingDuplicatedUrlWhileItIsInProgressShouldNotQueueItTwice() {

        whenCrawling("http://host1").discover("http://host1/dst1", "http://host1/dst2");

        //dst1 is never submitted to simulate that is taking longer than dst2 crawl
        doAnswer(invocation -> null).when(pool).accept(taskForUri("http://host1/dst1"));


        sut.start();


        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst1"));
        verify(pool).accept(taskForUri("http://host1/dst2"));
    }

    @Test
    public void canHandleInvalidEmptyJavascriptLinks() {
        whenCrawling("http://host1").discover("javascript:{}", "/dst1");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst1"));
    }


    @Test
    public void canHandleLinksRelativeToCurrentPage() {
        whenCrawling("http://host1").discover("http://host1/path/page");
        whenCrawling("http://host1/path/page").discover("relativeToCurrentPath");
        sut.start();
        verify(pool).accept(taskForUri("http://host1/path/relativeToCurrentPath"));
    }

    @Test
    public void canHandleInvalidLink() {
        whenCrawling("http://host1").discover("not +[a valid link", "/dst1");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/dst1"));
        verify(pool).accept(taskForUri("http://host1/not%20+%5Ba%20valid%20link"));
    }

    @Test
    public void canHandleNoLinksAtAll() {
        doAnswer(invocation -> {
            SnapshotTask task = invocation.getArgument(0);
            PageSnapshot t = aPageSnapshotWithStandardValuesForUri(task.getUri().toString());
            t.setLinks(null);
            task.getTaskRequester().accept(CrawlResult.forSnapshot(t));
            return null;
        }).when(pool).accept(taskForUri("http://host1"));

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));

    }

    @Test
    public void shouldIgnoreFragment() {
        whenCrawling("http://host1").discover("http://host1#someFragment");
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool, times(1)).accept(any());

    }


    @Test
    public void canManageLinkWithFragmentOnly() {
        whenCrawling("http://host1").discover("#");
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void canManageEmptyLinks() {
        whenCrawling("http://host1").discover("%20#");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldBeAbleToHandleURIWithEncodedSpaces() {
        whenCrawling("http://host1").discover("/this%20destination%20contains%20spaces");
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/this%20destination%20contains%20spaces"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldHandleUriWithLeadingSpaces() {
        whenCrawling("http://host1").discover("/leadingspaces    ");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/leadingspaces"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldBeTolerantWithLinksContainingSpacesInTheMiddle() {
        whenCrawling("http://host1").discover("/link with spaces");

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/link%20with%20spaces"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldEnqueueCanonicalLinkIfDifferentFromUri() {

        String baseUri = "http://host1/base?t=12345";
        String canonicalPath = "http://host1/base";

        initMocksToReturnCanonical(baseUri, canonicalPath);


        sut = initSut().withUris(baseUri).build();
        sut.start();


        verify(pool).accept(taskForUri(baseUri));
        verify(pool).accept(taskForUri(canonicalPath));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);

    }

    @Test
    public void shouldNotCrawlTwiceWhenCanonicalIsSameAsUri() {
        String baseUri = "http://host1/base";
        String canonicalPath = "http://host1/base";

        initMocksToReturnCanonical(baseUri, canonicalPath);

        sut = initSut().withUris(baseUri).build();
        sut.start();

        verify(pool).accept(taskForUri(baseUri));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }


    @Test
    public void shouldNotNotifyListenersForBlockedRedirectChains() {
        whenCrawling("http://host1").redirectToBlockedUrl("http://host1/blockedByRobots");

        sut.start();
        verify(dispatch).crawlEnded();
        verifyNoMoreInteractions(dispatch);
    }

    @Test
    public void shouldShutdownPoolWhenFinished() {

        whenCrawling("http://host1").discover("http://host1/path0");
        whenCrawling("http://host1/path0").discover("http://host1/path1");
        whenCrawling("http://host1/path1").discover("http://host1/path2");
        whenCrawling("http://host1/path2").discover("http://host1/path3");

        sut.start();

        InOrder inOrder = inOrder(pool);
        inOrder.verify(pool, times(5)).accept(any());
        inOrder.verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldDispatchCrawlEndedWhenFinished() {
        sut.start();
        verify(dispatch).crawlEnded();
    }

    @Test
    public void shouldNotCrawlMoreThanSpecified_discoverOneByOne() {
        whenCrawling("http://host1").discover("http://host1/1");
        whenCrawling("http://host1/1").discover("http://host1/2");
        whenCrawling("http://host1/2").discover("http://host1/3");
        whenCrawling("http://host1/3").discover("http://host1/should-not-get-here");

        sut = initSut().withMaxCrawls(4).build();
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/1"));
        verify(pool).accept(taskForUri("http://host1/2"));
        verify(pool).accept(taskForUri("http://host1/3"));
        verify(pool).shutDown();

        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldNotCrawlMoreThanSpecified_discoverAllAtOnce() {
        whenCrawling("http://host1").discover("http://host1/1", "http://host1/2", "http://host1/3", "http://host1/should-not-get-here");

        sut = initSut().withMaxCrawls(4).build();
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/1"));
        verify(pool).accept(taskForUri("http://host1/2"));
        verify(pool).accept(taskForUri("http://host1/3"));
        verify(pool).shutDown();

        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldNotCrawlDuplicateSeeds() {
        sut = initSut().withUris("http://host1/", "http://host1/").build();
        sut.start();
        verify(pool).accept(taskForUri("http://host1/"));
        verify(pool).shutDown();
        verifyNoMoreInteractions(pool);
    }


    @Test
    public void shouldRemoveUrlLongerThan1KBCountingDomain() {

        String verylongRelativelink = "/" + IntStream.range(0, MAX_URL_LEN - 1).mapToObj(sdi -> "0").collect(Collectors.joining());

        whenCrawling("http://host1").discover("http://host1/1", verylongRelativelink);

        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("http://host1/1"));

        verify(pool).shutDown();

        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldResolveLinksBasedOnDestinationUrl() {
        whenCrawling("http://host1").redirectTo("https://host1").discover("/1");
        sut.start();

        verify(pool).accept(taskForUri("http://host1"));
        verify(pool).accept(taskForUri("https://host1/1"));

        verify(pool).shutDown();

        verifyNoMoreInteractions(pool);
    }

    @Test
    public void shouldHandleEmptyRedirectChains() {
        //unknown host would return that, for example. But any SnapshotException can potentially return it
        whenCrawling("http://host1").returnEmptyRedirectChain();
        sut.start();
        verify(pool).accept(taskForUri("http://host1"));
    }

    private List<URI> uris(String... s) {
        return Stream.of(s).map(URI::create).collect(Collectors.toList());
    }

    private CrawlerQueueTestMockBuilder whenCrawling(String baseUri) {
        return new CrawlerQueueTestMockBuilder(baseUri);
    }


    private URI uri(String uri) {
        return URI.create(uri);
    }

    private void initMocksToReturnCanonical(String baseUri, String canonicalPath) {
        doAnswer(invocation -> {
            SnapshotTask task = invocation.getArgument(0);
            PageSnapshot t = aPageSnapshotWithStandardValuesForUri(task.getUri().toString());
            t.setCanonicals(Collections.singletonList(canonicalPath));

            CrawlResult result = CrawlResult.forSnapshot(t);
            task.getTaskRequester().accept(result);
            return null;
        }).when(pool).accept(taskForUri(baseUri));
    }

    private class CrawlerQueueTestMockBuilder {

        private final String baseUri;
        private String redirectUrl;

        CrawlerQueueTestMockBuilder(String baseUri) {
            this.baseUri = baseUri;
        }

        void discover(String... discoveredUris) {
            discover(Arrays.stream(discoveredUris).map(s -> new PageLink(s, emptyMap())).collect(Collectors.toList()));
        }

        void discover(List<PageLink> links) {
            doAnswer(invocation -> {
                SnapshotTask task = invocation.getArgument(0);
                String sourceUri = task.getUri().toString();
                PageSnapshot t = aPageSnapshotWithStandardValuesForUri(sourceUri);
                t.setLinks(links);
                if (redirectUrl != null) {
                    t.setRedirectChainElements(Arrays.asList(
                            new RedirectChainElement(sourceUri, 301, this.redirectUrl),
                            new RedirectChainElement(this.redirectUrl, 200, this.redirectUrl)));
                }
                task.getTaskRequester().accept(CrawlResult.forSnapshot(t));
                return null;
            }).when(pool).accept(taskForUri(baseUri));
        }

        public void redirectToBlockedUrl(String redirectDestination) {
            doAnswer(invocation -> {
                SnapshotTask task = invocation.getArgument(0);
                RedirectChain chain = new RedirectChain();
                chain.addElement(new RedirectChainElement(baseUri, 301, redirectDestination));
                task.getTaskRequester().accept(CrawlResult.forBlockedChain(chain));
                return null;
            }).when(pool).accept(taskForUri(baseUri));
        }

        public void returnEmptyRedirectChain() {
            doAnswer(invocation -> {
                SnapshotTask task = invocation.getArgument(0);
                PageSnapshot t = aPageSnapshotWithStandardValuesForUri(task.getUri().toString());
                t.setRedirectChainElements(Collections.emptyList());
                task.getTaskRequester().accept(CrawlResult.forSnapshot(t));
                return null;
            }).when(pool).accept(taskForUri(baseUri));
        }

        public CrawlerQueueTestMockBuilder redirectTo(String s) {
            this.redirectUrl = s;
            return this;
        }
    }

    private SnapshotTask taskForUri(String uri) {
        return argThat(arg -> arg.getUri().equals(uri(uri)));
    }


    private CrawlerQueueBuilder initSut() {
        return new CrawlerQueueBuilder();
    }

    private class CrawlerQueueBuilder {

        private String[] uris = new String[]{"http://host1"};
        private int maxCrawls = MAX_CRAWLS;
        private UriFilter filter = NO_URI_FILTER;

        public CrawlerQueueBuilder withUris(String... uris) {
            this.uris = uris;
            return this;
        }

        private CrawlerQueueBuilder withFilter(UriFilter filter) {
            this.filter = filter;
            return this;
        }

        public CrawlerQueue build() {
            return new CrawlerQueue(QUEUE_NAME, uris(uris), pool, filter, maxCrawls, dispatch);
        }

        public CrawlerQueueBuilder withMaxCrawls(int maxCrawls) {
            this.maxCrawls = maxCrawls;
            return this;
        }
    }
}
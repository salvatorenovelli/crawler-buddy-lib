package com.myseotoolbox.crawler.config;

import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.spider.CrawlEventDispatch;
import com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder;
import com.myseotoolbox.crawler.websitecrawl.CrawlStartedEvent;
import com.myseotoolbox.crawler.websitecrawl.WebsiteCrawl;
import com.myseotoolbox.crawler.websitecrawl.WebsiteCrawlFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.fail;


@RunWith(MockitoJUnitRunner.class)
public class CrawlEventDispatchTest {

    public static final String TEST_ORIGIN = "http://origin";
    public static final CrawlStartedEvent CRAWL_STARTED_EVENT = new CrawlStartedEvent(TEST_ORIGIN, Arrays.asList("/one", "/two"));

    public static final PageSnapshot TEST_PAGE_SNAPSHOT = PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri("http://host");
    public static final CrawlResult TEST_CRAWL_RESULT = CrawlResult.forSnapshot(TEST_PAGE_SNAPSHOT);
    public static final String TEST_CRAWL_ID = UUID.randomUUID().toString();
    private static final WebsiteCrawl CRAWL = WebsiteCrawlFactory.newWebsiteCrawlFor(TEST_CRAWL_ID, TEST_ORIGIN, Collections.emptyList());


    CrawlEventDispatch sut;

    @Before
    public void setUp() {
        sut = new CrawlEventDispatch(CRAWL);
    }

    @Test
    public void name() {
        fail("Not tested yet");
    }


    //
//    @Test
//    public void shouldNotifyPageCrawlListeners() {
//        sut.pageCrawled(TEST_CRAWL_RESULT);
//        verify(monitoredUriUpdater).updateCurrentValue(CRAWL, TEST_PAGE_SNAPSHOT);
//        verify(dispatch).pageCrawlCompletedEvent(TEST_CRAWL_ID_STR, TEST_PAGE_SNAPSHOT);
//        verify(linksListener).accept(TEST_CRAWL_RESULT);
//    }
//
//    @Test
//    public void shouldNotifyCrawlStartedListeners() {
//        sut.crawlStarted(CRAWL_STARTED_EVENT);
//        verify(websiteCrawlRepository).save(argThat(event -> {
//            assertThat(event.getId(), is(TEST_CRAWL_ID));
//            assertThat(event.getOrigin(), is(TEST_ORIGIN));
//            assertThat(event.getSeeds(), containsInAnyOrder("/one", "/two"));
//            return true;
//        }));
//    }
//
//    @Test
//    public void exceptionsInMonitoredUriUpdaterDoesNotPreventPersistenceOfCrawl() {
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(monitoredUriUpdater).updateCurrentValue(any(), any());
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(linksListener).accept(any());
//        sut.pageCrawled(TEST_CRAWL_RESULT);
//        verify(dispatch).pageCrawlCompletedEvent(TEST_CRAWL_ID_STR, TEST_PAGE_SNAPSHOT);
//    }
//
//    @Test
//    public void exceptionsInpageCrawlPersistenceDoesNotPreventMonitoredUriUpdate() {
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(dispatch).pageCrawlCompletedEvent(any(), any());
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(linksListener).accept(any());
//        sut.pageCrawled(TEST_CRAWL_RESULT);
//        verify(monitoredUriUpdater).updateCurrentValue(CRAWL, TEST_PAGE_SNAPSHOT);
//    }
//
//    @Test
//    public void exceptionsInLinksListenerDoesNotPreventMonitoredUriUpdate() {
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(dispatch).pageCrawlCompletedEvent(any(), any());
//        doThrow(new RuntimeException("This should not prevent update of the other")).when(monitoredUriUpdater).updateCurrentValue(any(), any());
//        sut.pageCrawled(TEST_CRAWL_RESULT);
//        verify(linksListener).accept(TEST_CRAWL_RESULT);
//    }
//
//    @Test
//    public void exceptionsInPageCrawlStartedShouldNotPropagate() {
//        doThrow(new RuntimeException("This should not propagate")).when(websiteCrawlRepository).save(any());
//        sut.crawlStarted(CRAWL_STARTED_EVENT);
//    }
//
//    @Test
//    public void shouldPublishCrawlEndedOnPubSub() {
//        sut.crawlEnded();
//        verify(dispatch).websiteCrawlCompletedEvent(CRAWL);
//    }

}
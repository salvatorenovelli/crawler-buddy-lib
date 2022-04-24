package com.myseotoolbox.crawler.spider;


import com.myseotoolbox.crawler.httpclient.SnapshotException;
import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;
import com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.net.URI.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CrawlJobTest {


    public static final UriFilter NO_URI_FILTER = (s, d) -> true;
    public static final URI TEST_ORIGIN = URI.create("http://domain1");
    public static final int MAX_CRAWLS = 1000;
    @Mock private WebPageReader pageReader;
    @Mock private CrawlEventListener dispatch;

    @Before
    public void setUp() throws Exception {
        when(pageReader.snapshotPage(any())).thenAnswer(arguments -> CrawlResult.forSnapshot(PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri(arguments.getArguments()[0].toString())));
    }

    @Test
    public void shouldFilterOutSeedsFromOutsideOrigin() {
        CrawlJob sut = initSut().withSeeds("http://domain1", "http://domain2").build();
        sut.start();
        Mockito.verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://domain1")));
    }

    @Test
    public void shouldNotifySubscribers() {
        CrawlJob sut = initSut().build();
        sut.start();
        Mockito.verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://domain1")));
    }

    @Test
    public void shouldOnlyVisitSeedsNotTheRoot() {
        CrawlJob sut = initSut().withSeeds("http://domain1/path1", "http://domain1/path2").build();
        sut.start();
        Mockito.verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://domain1/path1")));
        Mockito.verify(dispatch).pageCrawled(argThat(argument -> argument.getUri().equals("http://domain1/path2")));
        verify(dispatch, atMost(2)).pageCrawled(any());

    }

    @Test
    public void shouldNotVisitDuplicateSeeds() throws SnapshotException {
        CrawlJob sut = initSut().withSeeds("http://domain1", "http://domain1").build();
        sut.start();
        verify(pageReader).snapshotPage(create("http://domain1"));
        verifyNoMoreInteractions(pageReader);
    }

    @Test
    public void shouldNotifyForCrawlStarted() {

        String[] seeds = {"http://domain1/a", "http://domain1/b"};
        CrawlJob sut = initSut().withSeeds(seeds).build();


        sut.start();

        verify(dispatch).crawlStarted(argThat(conf -> {
            assertThat(conf.getOrigin(), is(TEST_ORIGIN.toString()));
            assertThat(conf.getSeeds(), containsInAnyOrder(seeds));
            return true;
        }));
    }


    private CrawlJobBuilder initSut() {
        return new CrawlJobBuilder();
    }

    private class CrawlJobBuilder {
        private List<URI> seeds = Arrays.asList(create("http://domain1"));

        public CrawlJobBuilder withSeeds(String... seeds) {
            this.seeds = Arrays.stream(seeds).map(URI::create).collect(Collectors.toList());
            return this;
        }

        public CrawlJob build() {
            return new CrawlJob(TEST_ORIGIN, seeds, pageReader, NO_URI_FILTER, new CurrentThreadTestExecutorService(), MAX_CRAWLS, dispatch);

        }
    }
}
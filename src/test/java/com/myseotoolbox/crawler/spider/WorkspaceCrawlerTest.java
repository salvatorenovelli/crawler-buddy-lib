package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.config.PageCrawlListener;
import com.myseotoolbox.crawler.model.CrawlerSettings;
import com.myseotoolbox.crawler.model.Workspace;
import com.myseotoolbox.crawler.repository.WebsiteCrawlLogRepository;
import com.myseotoolbox.crawler.repository.WorkspaceRepository;
import com.myseotoolbox.crawler.spider.model.WebsiteCrawlLog;
import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static com.myseotoolbox.crawler.spider.WorkspaceCrawler.MAX_CONCURRENT_CONNECTIONS_PER_DOMAIN;
import static java.net.URI.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceCrawlerTest {


    private static final int YESTERDAY = -1;
    private static final int TWO_DAYS_AGO = -2;
    public static final int DEFAULT_CRAWL_VALUE_WHEN_MISSING = CrawlerSettings.MIN_CRAWL_INTERVAL;
    public static final int MAX_CRAWLS = 100;
    private final List<CrawlJob> mockJobs = new ArrayList<>();
    private final List<Workspace> allWorkspaces = new ArrayList<>();
    private final List<WebsiteCrawlLog> crawlLogs = new ArrayList<>();

    @Mock private CrawlJobFactory crawlFactory;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WebsiteCrawlLogRepository websiteCrawlLogRepository;
    @Mock private PageCrawlListener pageCrawlListener;

    WorkspaceCrawler sut;
    @Spy private Executor executor = new CurrentThreadTestExecutorService();

    @Before
    public void setUp() {
        sut = new WorkspaceCrawler(workspaceRepository, crawlFactory, websiteCrawlLogRepository, pageCrawlListener, executor);

        when(crawlFactory.build(any(URI.class), anyList(), anyInt(), anyInt(), any())).thenAnswer(
                invocation -> {
                    CrawlJob mock = mock(CrawlJob.class);
                    mockJobs.add(mock);
                    return mock;
                }
        );

        when(workspaceRepository.findAll()).thenReturn(allWorkspaces);
    }

    @Test
    public void nullCrawlerSettingsDontStopOtherCrawls() {
        givenAWorkspace().withWebsiteUrl("http://host1").build();
        givenAWorkspace().withCrawlerSettings(null).build();

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host1");

    }

    @Test
    public void shouldCrawlAllTheWorkspaces() {
        givenAWorkspace().withWebsiteUrl("http://host1").build();
        givenAWorkspace().withWebsiteUrl("http://host2").build();

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host1");
        crawlStartedFor("http://host2");
    }

    @Test
    public void shouldNotCrawlTwice() {
        givenAWorkspace().withWebsiteUrl("http://host1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/").build();

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host1");
        verifyNoMoreCrawls();
    }

    @Test
    public void shouldGroupByOrigin() {
        givenAWorkspace().withWebsiteUrl("http://host1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path2/").build();
        givenAWorkspace().withWebsiteUrl("http://host2").build();

        sut.crawlAllWorkspaces();

        crawlStartedForOriginWithSeeds("http://host1", asList("http://host1", "http://host1/path1", "http://host1/path2"));
        crawlStartedForOriginWithSeeds("http://host2", asList("http://host2"));
    }

    @Test
    public void shouldNotHaveDuplicatesInSeeds() {
        givenAWorkspace().withWebsiteUrl("http://host1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path2").build();

        sut.crawlAllWorkspaces();

        crawlStartedForOriginWithSeeds("http://host1", asList("http://host1", "http://host1/path1", "http://host1/path2"));
        verifyNoMoreCrawls();
    }

    @Test
    public void shouldNotTryToBuildInvalidUrl() {
        givenAWorkspace().withWebsiteUrl("TBD").build();

        sut.crawlAllWorkspaces();

        verifyNoMoreCrawls();
    }

    @Test
    public void numConnectionsIsGreaterIfWeHaveMultipleSeeds() {
        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path2").build();

        sut.crawlAllWorkspaces();

        websiteCrawledWithConcurrentConnections(2);
    }

    @Test
    public void shouldNeverUseMoreThanMaxConnections() {
        for (int i = 0; i < MAX_CONCURRENT_CONNECTIONS_PER_DOMAIN * 2; i++) {
            givenAWorkspace().withWebsiteUrl("http://host1/path" + i).build();
        }

        sut.crawlAllWorkspaces();

        websiteCrawledWithConcurrentConnections(MAX_CONCURRENT_CONNECTIONS_PER_DOMAIN);

    }

    @Test
    public void numConnectionsOnlyCountsUniqueSeeds() {

        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path1/").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path1").build();
        givenAWorkspace().withWebsiteUrl("http://host1/path2").build();

        sut.crawlAllWorkspaces();


        websiteCrawledWithConcurrentConnections(2);
    }

    @Test
    public void exceptionInBuildOrStartShouldNotPreventOtherCrawls() {
        String originWithException = "http://host1/";

        givenAWorkspace().withWebsiteUrl(originWithException).build();
        givenAWorkspace().withWebsiteUrl("http://host2/").build();


        when(crawlFactory.build(eq(create(originWithException)), anyList(), anyInt(), anyInt(), any())).thenThrow(new RuntimeException("Testing exceptions"));

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host2");
    }

    @Test
    public void shouldOnlyCrawlWhereCrawlingIsEnabled() {
        givenAWorkspace().withWebsiteUrl("http://host2/").withCrawlingDisabled().build();
        sut.crawlAllWorkspaces();
        verifyNoMoreCrawls();
    }


    @Test
    public void shouldConsiderPathForCrawlInterval() {
        givenAWorkspace().withWebsiteUrl("http://host1/abc").withCrawlingIntervalOf(2).withLastCrawlHappened(YESTERDAY).build();
        givenAWorkspace().withWebsiteUrl("http://host1/cde").withCrawlingIntervalOf(1).withLastCrawlHappened(YESTERDAY).build();

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host1/cde");
        verifyNoMoreCrawls();
    }

    @Test
    public void shouldOnlyCrawlAtConfiguredInterval() {
        givenAWorkspace().withWebsiteUrl("http://host1/").withCrawlingIntervalOf(1).withLastCrawlHappened(YESTERDAY).build();
        givenAWorkspace().withWebsiteUrl("http://host2/").withCrawlingIntervalOf(2).withLastCrawlHappened(TWO_DAYS_AGO).build();
        givenAWorkspace().withWebsiteUrl("http://host3/").withCrawlingIntervalOf(3).withLastCrawlHappened(TWO_DAYS_AGO).build();
        givenAWorkspace().withWebsiteUrl("http://host4/").withCrawlingIntervalOf(3).withLastCrawlHappened(YESTERDAY).build();

        sut.crawlAllWorkspaces();

        crawlStartedFor("http://host1");
        crawlStartedFor("http://host2");

        verifyNoMoreCrawls();
    }

    @Test
    public void canHandleNoLastCrawl() {
        givenAWorkspace().withWebsiteUrl("http://host1/").withCrawlingIntervalOf(1).build();
        sut.crawlAllWorkspaces();
        crawlStartedFor("http://host1");
    }

    @Test
    public void canHandleNoCrawlIntervalSpecified() {
        givenAWorkspace().withWebsiteUrl("http://host1/").withCrawlingIntervalOf(DEFAULT_CRAWL_VALUE_WHEN_MISSING).withLastCrawlHappened(YESTERDAY).build();
        sut.crawlAllWorkspaces();
        crawlStartedFor("http://host1");
    }

    @Test
    public void shouldPersistLastCrawl() {
        givenAWorkspace().withWebsiteUrl("http://host1/").withCrawlingIntervalOf(1).build();
        sut.crawlAllWorkspaces();
        verify(websiteCrawlLogRepository).save(argThat(argument -> argument.getOrigin().equals("http://host1/") && argument.getDate() != null));
    }

    @Test
    public void shouldNotPersistTwice() {
        givenAWorkspace().withWebsiteUrl("http://host1/abc").withCrawlingIntervalOf(1).build();
        givenAWorkspace().withWebsiteUrl("http://host1/abc/").withCrawlingIntervalOf(1).build();

        sut.crawlAllWorkspaces();

        verify(websiteCrawlLogRepository).save(argThat(argument -> argument.getOrigin().equals("http://host1/abc/") && argument.getDate() != null));
        verify(websiteCrawlLogRepository, times(2)).findTopByOriginOrderByDateDesc(anyString());

        verifyNoMoreInteractions(websiteCrawlLogRepository);
    }

    @Test
    public void shouldPersistLastCrawlShouldSaveBaseDomain() {
        givenAWorkspace().withWebsiteUrl("http://host1/abc/").withCrawlingIntervalOf(1).build();
        sut.crawlAllWorkspaces();

        System.out.println(mockingDetails(websiteCrawlLogRepository).printInvocations());

        verify(websiteCrawlLogRepository).save(argThat(argument -> argument.getOrigin().equals("http://host1/abc/") && argument.getDate() != null));
    }

    @Test
    public void shouldExecuteEveryCrawlInADifferentThread() {
        givenAWorkspace().withWebsiteUrl("http://host1").withCrawlingIntervalOf(1).build();
        givenAWorkspace().withWebsiteUrl("http://host2").withCrawlingIntervalOf(1).build();

        sut.crawlAllWorkspaces();

        verify(executor, times(2)).execute(any());
    }

    private void websiteCrawledWithConcurrentConnections(int numConnections) {
        verify(crawlFactory).build(any(URI.class), anyList(), eq(numConnections), anyInt(), any());
    }

    private void crawlStartedFor(String origin) {
        crawlStartedForOriginWithSeeds(addTrailingSlashIfMissing(origin), singletonList(origin));
    }

    private void crawlStartedForOriginWithSeeds(String origin, List<String> seeds) {
        Object[] expectedSeeds = seeds.stream().map(this::addTrailingSlashIfMissing).map(URI::create).toArray();

        try {
            verify(crawlFactory).build(eq(create(origin).resolve("/")),
                    argThat(argument -> new HamcrestArgumentMatcher<>(containsInAnyOrder(expectedSeeds)).matches(argument)),
                    anyInt(), anyInt(), any());

            mockJobs.forEach(job -> verify(job).start());
        } catch (Throwable e) {
            System.out.println(mockingDetails(crawlFactory).printInvocations());
            throw e;
        }
    }

    private void verifyNoMoreCrawls() {
        try {
            verifyNoMoreInteractions(crawlFactory);
            mockJobs.forEach(Mockito::verifyNoMoreInteractions);
        } catch (Throwable e) {
            System.out.println(mockingDetails(crawlFactory).printInvocations());
            throw e;
        }
    }

    private WorkspaceBuilder givenAWorkspace() {
        return new WorkspaceBuilder();
    }

    private class WorkspaceBuilder {


        private final Workspace curWorkspace;

        private WorkspaceBuilder() {
            curWorkspace = new Workspace();
            curWorkspace.setCrawlerSettings(new CrawlerSettings(1, true, 1));
        }

        public WorkspaceBuilder withWebsiteUrl(String s) {
            curWorkspace.setWebsiteUrl(s);
            return this;
        }

        public void build() {
            allWorkspaces.add(curWorkspace);
            when(websiteCrawlLogRepository
                    .findTopByOriginOrderByDateDesc(curWorkspace.getWebsiteUrl()))
                    .thenAnswer(invocation -> crawlLogs.stream()
                            .filter(websiteCrawlLog -> websiteCrawlLog.getOrigin().equals(invocation.getArgument(0)))
                            .findFirst());
        }

        public WorkspaceBuilder withCrawlingDisabled() {
            CrawlerSettings s = curWorkspace.getCrawlerSettings();
            curWorkspace.setCrawlerSettings(new CrawlerSettings(s.getMaxConcurrentConnections(), false, s.getCrawlIntervalDays()));
            return this;
        }

        public WorkspaceBuilder withCrawlingIntervalOf(int days) {
            CrawlerSettings s = curWorkspace.getCrawlerSettings();
            curWorkspace.setCrawlerSettings(new CrawlerSettings(s.getMaxConcurrentConnections(), s.isCrawlEnabled(), days));
            return this;
        }

        public WorkspaceBuilder withLastCrawlHappened(int dayOffset) {
            crawlLogs.add(new WebsiteCrawlLog(curWorkspace.getWebsiteUrl(), LocalDate.now().plusDays(dayOffset)));
            return this;
        }

        public WorkspaceBuilder withCrawlerSettings(CrawlerSettings settings) {
            curWorkspace.setCrawlerSettings(settings);
            return this;
        }
    }

    private String addTrailingSlashIfMissing(String uri) {
        return uri + (uri.endsWith("/") ? "" : "/");
    }
}
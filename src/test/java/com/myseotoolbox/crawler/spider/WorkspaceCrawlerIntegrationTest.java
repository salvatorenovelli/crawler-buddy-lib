//package com.myseotoolbox.crawler.spider;
//
//import com.myseotoolbox.crawler.httpclient.*;
//import com.myseotoolbox.crawler.model.CrawlResult;
//import com.myseotoolbox.crawler.repository.WebsiteCrawlLogRepository;
//import com.myseotoolbox.crawler.repository.WorkspaceRepository;
//import com.myseotoolbox.crawler.spider.configuration.CrawlerSettingsBuilder;
//import com.myseotoolbox.crawler.spider.configuration.RobotsTxtAggregation;
//import com.myseotoolbox.crawler.spider.sitemap.SitemapReader;
//import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;
//import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadPoolExecutor;
//
//import static org.mockito.Mockito.*;
//
//
//@RunWith(MockitoJUnitRunner.class)
//public class WorkspaceCrawlerIntegrationTest {
//
//
//    private TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();
//
//    private Executor executor = new CurrentThreadTestExecutorService();
//    private CrawlExecutorFactory testExecutorBuilder = new CurrentThreadCrawlExecutorFactory();
//    private WebPageReaderFactory webPageReaderFactory = new WebPageReaderFactory(new HttpRequestFactory(new HttpURLConnectionFactory()));
//    private WebsiteUriFilterFactory uriFilterFactory = new WebsiteUriFilterFactory();
//    private SitemapReader sitemapReader = new SitemapReader();
//    private RobotsTxtAggregation robotsAggregation = new RobotsTxtAggregation(new HTTPClient());
//
//
//    @Mock private WorkspaceRepository workspaceRepository;
//    @Mock private WebsiteCrawlLogRepository websiteCrawlLogRepository;
//    @Mock private CrawlEventDispatch dispatch;
//    @Mock private CrawlEventDispatchFactory listenerProvider;
//
//    private List<Workspace> allWorkspaces = new ArrayList<>();
//    private CrawlJobFactory crawlJobFactory;
//    private WorkspaceCrawler sut;
//
//
//    @Before
//    public void setUp() throws Exception {
//        when(listenerProvider.get(any())).thenReturn(dispatch);
//        when(workspaceRepository.findAll()).thenReturn(allWorkspaces);
//        crawlJobFactory = new CrawlJobFactory(webPageReaderFactory, uriFilterFactory, testExecutorBuilder, sitemapReader);
//        sut = new WorkspaceCrawler(workspaceRepository, crawlJobFactory, websiteCrawlLogRepository, listenerProvider, robotsAggregation, executor);
//        testWebsiteBuilder.run();
//    }
//
//
//    public void crawlSingleUrlExploratory() {
//        WebPageReader build = webPageReaderFactory.build((sourceUri, discoveredLink) -> true);
//
//        CrawlResult crawlResult = null;
//        try {
//            crawlResult = build.snapshotPage(URI.create(""));
//        } catch (SnapshotException e) {
//            System.out.println(e.getPartialSnapshot().getRedirectChainElements());
//        }
//
//        System.out.println(crawlResult);
//
//    }
//
//    @Test
//    public void shouldNotifyListenerOfAllThePageCrawled() {
//        givenAWorkspace().withWebsiteUrl(testUri("/").toString()).build();
//        givenAWebsite().havingRootPage().withLinksTo("/page1", "page2").save();
//
//        sut.crawlAllWorkspaces();
//
//        verify(dispatch).pageCrawled(crawlResultFor("/"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page1"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page2"));
//        verify(dispatch, atMost(3)).pageCrawled(any());
//
//    }
//
//    @Test
//    public void shouldUseRobotsTxt() {
//        givenAWorkspace().withWebsiteUrl(testUri("/").toString()).build();
//        givenAWebsite()
//                .havingRootPage().withLinksTo("/page1", "page2")
//                .withRobotsTxt().userAgent("*").disallow("/page2").build().save();
//
//        sut.crawlAllWorkspaces();
//
//        verify(dispatch).pageCrawled(crawlResultFor("/"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page1"));
//        verify(dispatch, atMost(2)).pageCrawled(any());
//    }
//
//
//    @Test
//    public void shouldNotCrawlNoFollow() {
//        givenAWorkspace().withWebsiteUrl(testUri("/").toString()).build();
//        givenAWebsite()
//                .havingRootPage()
//                .withLinksTo("/page1")
//                .withNoFollowLinksTo("/page2").save();
//
//        sut.crawlAllWorkspaces();
//
//        verify(dispatch).pageCrawled(crawlResultFor("/"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page1"));
//        verify(dispatch, atMost(2)).pageCrawled(any());
//    }
//
//    @Test
//    public void shouldDisableRobotsTxt() {
//
//        givenAWorkspace().withWebsiteUrl(testUri("/").toString())
//                .withCrawlerSettings(CrawlerSettingsBuilder.defaultSettings().withIgnoreRobotsTxt(true).build()).build();
//
//        givenAWebsite()
//                .havingRootPage().withLinksTo("/page1", "page2")
//                .withRobotsTxt().userAgent("*").disallow("/page2").build().save();
//
//        sut.crawlAllWorkspaces();
//
//        verify(dispatch).pageCrawled(crawlResultFor("/"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page1"));
//        verify(dispatch).pageCrawled(crawlResultFor("/page2"));
//        verify(dispatch, atMost(3)).pageCrawled(any());
//
//    }
//
//    @Test
//    public void shouldOnlyCrawlInsidePath() {
//        givenAWorkspace().withWebsiteUrl(testUri("/path/").toString()).build();
//
//        givenAWebsite()
//                .havingPage("/path/").withLinksTo("/path/first", "/path/subpath/second", "/outside/something").and()
//                .havingPage("/outside/something").withLinksTo("/outside/shouldnotcrawlthis")
//                .save();
//        sut.crawlAllWorkspaces();
//
//
//        verify(dispatch).pageCrawled(crawlResultFor("/path/"));
//        verify(dispatch).pageCrawled(crawlResultFor("/path/first"));
//        verify(dispatch).pageCrawled(crawlResultFor("/path/subpath/second"));
//        verify(dispatch).pageCrawled(crawlResultFor("/outside/something"));//still crawl this as the link originates on a page where we want to check for broken links, for example
//        verify(dispatch, atMost(4)).pageCrawled(any());
//
//    }
//
//    @Test
//    public void ifTheSeedsHaveFilenamesAtTheEndTheyShouldNotBeConsideredPath() {
//
//        givenAWorkspace().withWebsiteUrl(testUri("/path/index.html").toString()).build();
//        givenAWebsite()
//                .havingPage("/path/index.html").withLinksTo("/path/first").save();
//        sut.crawlAllWorkspaces();
//
//
//        verify(dispatch).pageCrawled(crawlResultFor("/path/index.html"));
//        verify(dispatch).pageCrawled(crawlResultFor("/path/first"));
//        verify(dispatch, atMost(2)).pageCrawled(any());
//    }
//
//    private CrawlResult crawlResultFor(String s) {
//        return argThat(snapshot -> snapshot.getUri().equals(testUri(s).toString()));
//    }
//
//
//    private class CurrentThreadCrawlExecutorFactory extends CrawlExecutorFactory {
//        @Override
//        public ThreadPoolExecutor buildExecutor(String namePostfix, int concurrentConnections) {
//            return new CurrentThreadTestExecutorService();
//        }
//    }
//
//    private URI testUri(String url) {
//        return testWebsiteBuilder.buildTestUri(url);
//    }
//
//    private TestWebsiteBuilder givenAWebsite() {
//        return testWebsiteBuilder;
//    }
//
//
//    private TestWorkspaceBuilder givenAWorkspace() {
//        return new TestWorkspaceBuilder(allWorkspaces, websiteCrawlLogRepository);
//    }
//
//}

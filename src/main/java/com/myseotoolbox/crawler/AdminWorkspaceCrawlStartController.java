//package com.myseotoolbox.crawler;
//
//
//import com.myseotoolbox.crawler.httpclient.HTTPClient;
//import com.myseotoolbox.crawler.repository.WorkspaceRepository;
//import com.myseotoolbox.crawler.spider.CrawlEventDispatch;
//import com.myseotoolbox.crawler.spider.CrawlJob;
//import com.myseotoolbox.crawler.spider.CrawlJobFactory;
//import com.myseotoolbox.crawler.spider.WorkspaceCrawler;
//import com.myseotoolbox.crawler.spider.configuration.CrawlJobConfiguration;
//import com.myseotoolbox.crawler.spider.configuration.CrawlerSettings;
//import com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.DefaultRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.EmptyRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.IgnoredRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//import java.net.URI;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static com.myseotoolbox.crawler.websitecrawl.WebsiteCrawlFactory.newWebsiteCrawlFor;
//
//@RestController
//public class AdminWorkspaceCrawlStartController {
//
//    private final CrawlJobFactory factory;
//    private final WorkspaceRepository repository;
//    private final WorkspaceCrawler workspaceCrawler;
//    private final CrawlEventDispatchFactory crawlEventDispatchFactory;
//    private final HTTPClient client;
//
//    public AdminWorkspaceCrawlStartController(CrawlJobFactory factory, WorkspaceRepository repository, WorkspaceCrawler workspaceCrawler, CrawlEventDispatchFactory crawlEventDispatchFactory, HTTPClient client) {
//        this.factory = factory;
//        this.repository = repository;
//        this.workspaceCrawler = workspaceCrawler;
//        this.crawlEventDispatchFactory = crawlEventDispatchFactory;
//        this.client = client;
//    }
//
//    @GetMapping("/crawl-origin")
//    public String crawlOrigin(@RequestParam("seeds") List<String> seeds, @RequestParam(value = "numConnections", defaultValue = "1") int numConnections) throws IOException {
//        URI origin = WebsiteOriginUtils.extractOrigin(URI.create(seeds.get(0)));
//        List<URI> seedsAsUri = seeds.stream().map(URI::create).collect(Collectors.toList());
//
//
//        CrawlJobConfiguration configuration = getConfiguration(origin, seedsAsUri, numConnections, true);
//
//
//        CrawlJob job = factory.build(configuration, getCrawlEventsListener(origin));
//        job.start();
//        return "Crawling " + seeds + " with " + numConnections + " parallel connections. Started on " + new Date();
//    }
//
//    @GetMapping("/crawl-workspace")
//    public String crawlWorkspace(@RequestParam("seqNumber") int seqNumber, @RequestParam(value = "numConnections", defaultValue = "1") int numConnections) throws EntityNotFoundException, IOException {
//        Workspace ws = repository.findTopBySeqNumber(seqNumber).orElseThrow(EntityNotFoundException::new);
//        URI origin = URI.create(ws.getWebsiteUrl());
//
//        CrawlJobConfiguration conf = getConfiguration(origin, Collections.singletonList(origin), numConnections, shouldIgnoreRobotsTxt(ws));
//        CrawlJob job = factory.build(conf, getCrawlEventsListener(origin));
//
//        job.start();
//        return "Crawling " + ws.getWebsiteUrl() + " with " + numConnections + " parallel connections. Started on " + new Date() + "\n";
//    }
//
//    @GetMapping("/crawl-all-workspaces")
//    public String crawlAllWorkspaces() {
//        workspaceCrawler.crawlAllWorkspaces();
//        return "Started on " + new Date() + "\n";
//    }
//
//    private CrawlJobConfiguration getConfiguration(URI origin, List<URI> seeds, int numConnections, boolean ignoreRobots) throws IOException {
//        CrawlJobConfiguration.Builder builder = CrawlJobConfiguration
//                .newConfiguration(origin)
//                .withConcurrentConnections(numConnections)
//                .withSeeds(seeds);
//
//        RobotsTxt robotsTxt = buildRobotsTxt(origin, ignoreRobots);
//
//        builder.withRobotsTxt(robotsTxt);
//
//        return builder.build();
//    }
//
//    private RobotsTxt buildRobotsTxt(URI origin, boolean ignoreRobots) {
//        try {
//            String content = client.get(origin.resolve("/robots.txt"));
//            if (ignoreRobots) {
//                return new IgnoredRobotsTxt(origin.toString(), content.getBytes());
//            } else {
//                return new DefaultRobotsTxt(origin.toString(), content.getBytes());
//            }
//        } catch (IOException e) {
//            return new EmptyRobotsTxt(origin);
//        }
//    }
//
//
//    private boolean shouldIgnoreRobotsTxt(Workspace ws) {
//        CrawlerSettings crawlerSettings = ws.getCrawlerSettings();
//        return crawlerSettings != null && crawlerSettings.getFilterConfiguration() != null &&
//                crawlerSettings.getFilterConfiguration().shouldIgnoreRobotsTxt();
//    }
//
//    private CrawlEventDispatch getCrawlEventsListener(URI origin) {
//        return crawlEventDispatchFactory.get(newWebsiteCrawlFor(origin.toString(), Collections.emptyList()));
//    }
//}

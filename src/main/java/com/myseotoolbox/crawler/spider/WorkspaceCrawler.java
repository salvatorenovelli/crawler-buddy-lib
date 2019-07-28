package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.CrawlEventDispatchFactory;
import com.myseotoolbox.crawler.model.Workspace;
import com.myseotoolbox.crawler.repository.WebsiteCrawlLogRepository;
import com.myseotoolbox.crawler.repository.WorkspaceRepository;
import com.myseotoolbox.crawler.spider.configuration.CrawlJobConfiguration;
import com.myseotoolbox.crawler.spider.configuration.CrawlerSettings;
import com.myseotoolbox.crawler.spider.configuration.RobotsTxtAggregation;
import com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils;
import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
import com.myseotoolbox.crawler.spider.model.WebsiteCrawlLog;
import com.myseotoolbox.crawler.websitecrawl.WebsiteCrawl;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.myseotoolbox.crawler.utils.FunctionalExceptionUtils.runOrLogWarning;

@Slf4j
@Component
public class WorkspaceCrawler {


    private final WorkspaceRepository workspaceRepository;
    private final CrawlJobFactory crawlJobFactory;
    private final WebsiteCrawlLogRepository websiteCrawlLogRepository;
    private final CrawlEventDispatchFactory crawlEventDispatchFactory;
    private final Executor executor;
    private final RobotsTxtAggregation robotsTxtAggregation;

    public WorkspaceCrawler(WorkspaceRepository workspaceRepository,
                            CrawlJobFactory crawlJobFactory,
                            WebsiteCrawlLogRepository websiteCrawlLogRepository,
                            CrawlEventDispatchFactory crawlEventDispatchFactory,
                            RobotsTxtAggregation robotsTxtAggregation,
                            @Qualifier("crawl-job-init-executor") Executor executor) {
        this.workspaceRepository = workspaceRepository;
        this.crawlJobFactory = crawlJobFactory;
        this.websiteCrawlLogRepository = websiteCrawlLogRepository;
        this.crawlEventDispatchFactory = crawlEventDispatchFactory;
        this.executor = executor;
        this.robotsTxtAggregation = robotsTxtAggregation;
    }


    public void crawlAllWorkspaces() {

        log.info("Starting workspaces crawl...");

        Map<URI, Set<Workspace>> workspacesByHost = workspaceRepository.findAll()
                .stream()
                .filter(this::validOrigin)
                .filter(this::shouldCrawl)
                .collect(Collectors.groupingBy(this::extractOrigin, Collectors.toSet()));

        workspacesByHost.forEach((origin, workspaces) ->
                executor.execute(() -> runOrLogWarning(() -> {

                    Set<URI> seeds = extractSeeds(workspaces);

                    log.info("Starting crawl for {} with seeds: {}", origin, seeds);

                    RobotsTxt merged = robotsTxtAggregation.aggregate(workspaces);

                    CrawlJobConfiguration conf = CrawlJobConfiguration
                            .newConfiguration(origin)
                            .withSeeds(seeds)
                            .withConcurrentConnections(seeds.size())
                            .withRobotsTxt(merged)
                            .build();

                    CrawlEventDispatch dispatch = crawlEventDispatchFactory.get(generateCrawl(origin.toString(), seeds));

                    CrawlJob job = crawlJobFactory.build(conf, dispatch);
                    job.start();
                    //TODO: this needs to go
                    seeds.forEach(seed -> websiteCrawlLogRepository.save(new WebsiteCrawlLog(seed.toString(), LocalDate.now())));
                }, "Error while starting crawl for: " + origin))
        );


    }

    private WebsiteCrawl generateCrawl(String origin, Collection<URI> seeds) {
        return new WebsiteCrawl(new ObjectId(), origin, LocalDateTime.now(), seeds.stream().map(URI::toString).collect(Collectors.toList()));
    }

    private Set<URI> extractSeeds(Set<Workspace> workspaces) {
        return workspaces.stream().map(Workspace::getWebsiteUrl).map(this::addTrailingSlashIfMissing).map(URI::create).collect(Collectors.toSet());
    }

    private URI extractOrigin(Workspace workspace) {
        return WebsiteOriginUtils.extractOrigin(URI.create(addTrailingSlashIfMissing(workspace.getWebsiteUrl())));
    }

    private boolean shouldCrawl(Workspace workspace) {
        CrawlerSettings crawlerSettings = workspace.getCrawlerSettings();
        return crawlerSettings != null && crawlerSettings.isCrawlEnabled() && isDelayExpired(workspace);
    }

    private boolean validOrigin(Workspace workspace) {
        return WebsiteOriginUtils.isValidOrigin(workspace.getWebsiteUrl());
    }

    private boolean isDelayExpired(Workspace workspace) {

        return websiteCrawlLogRepository.findTopByOriginOrderByDateDesc(workspace.getWebsiteUrl()).map(lastCrawl -> {
            int crawlIntervalDays = workspace.getCrawlerSettings().getCrawlIntervalDays();
            boolean delayExpired = LocalDate.now().minusDays(crawlIntervalDays).compareTo(lastCrawl.getDate()) >= 0;

            if (!delayExpired) {
                log.info("Workspace {} doesnt need crawl yet. Crawl interval: {} Last Crawl: {}",
                        workspace.getOwnerName() + " - " + workspace.getName(),
                        crawlIntervalDays,
                        lastCrawl.getDate());
            }

            return delayExpired;
        }).orElse(true);
    }


    private String addTrailingSlashIfMissing(String uri) {
        return uri + (uri.endsWith("/") ? "" : "/");
    }
}

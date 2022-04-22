//package com.myseotoolbox.crawler.spider.configuration;
//
//import com.myseotoolbox.crawler.httpclient.HTTPClient;
//import com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.DefaultRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.EmptyRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.IgnoredRobotsTxt;
//import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Objects;
//
//@Slf4j
//@Component
//public class RobotsTxtAggregation {
//
//    private final HTTPClient httpClient;
//
//    public RobotsTxtAggregation(HTTPClient httpClient) {
//        this.httpClient = httpClient;
//    }
//
//
//    public RobotsTxt mergeConfigurations(Collection<Workspace> workspaces) {
//        Workspace workspace0 = new ArrayList<>(workspaces).get(0);
//        URI origin = WebsiteOriginUtils.extractOrigin(URI.create(workspace0.getWebsiteUrl()));
//
//        try {
//            String content = httpClient.get(origin.resolve("/robots.txt"));
//            if (anyWorkspaceIgnoreRobots(workspaces))
//                return new IgnoredRobotsTxt(origin.toString(), content.getBytes());
//
//            return new DefaultRobotsTxt(origin.toString(), content.getBytes());
//        } catch (IOException e) {
//            log.info("Unable to fetch RobotsTxt for {}. {}", workspaces, e.toString());
//            return new EmptyRobotsTxt(origin);
//        }
//    }
//
//    private boolean anyWorkspaceIgnoreRobots(Collection<Workspace> workspaces) {
//        return workspaces.stream()
//                .map(Workspace::getCrawlerSettings).filter(Objects::nonNull)
//                .map(CrawlerSettings::getFilterConfiguration).filter(Objects::nonNull)
//                .anyMatch(FilterConfiguration::shouldIgnoreRobotsTxt);
//    }
//}

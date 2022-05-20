package com.myseotoolbox.crawler.spider.filter.robotstxt;

import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.List;

import static com.myseotoolbox.crawler.httpclient.HttpGetRequest.BOT_NAME;

@Log4j2
public class DefaultRobotsTxt implements RobotsTxt {

    private final SimpleRobotRules robotRules;

    public DefaultRobotsTxt(String websiteOrigin, byte[] content) {
        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
        this.robotRules = parser.parseContent(websiteOrigin, content, null, BOT_NAME);
        if (robotRules.getRobotRules().size() < 1) log.warn("robots.txt was empty for {}", websiteOrigin);
    }

    @Override
    public boolean shouldCrawl(URI ignored, URI discoveredLink) {
        boolean allowed = robotRules.isAllowed(discoveredLink.toString());
        if (!allowed) log.debug("Blocked: ROBOTS  URI: {}", discoveredLink);
        return allowed;
    }

    public List<String> getSitemaps() {
        return this.robotRules.getSitemaps();
    }
}

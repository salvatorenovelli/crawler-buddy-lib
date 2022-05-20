package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.HTTPClient;
import com.myseotoolbox.crawler.spider.filter.robotstxt.DefaultRobotsTxt;
import com.myseotoolbox.crawler.spider.filter.robotstxt.EmptyRobotsTxt;
import com.myseotoolbox.crawler.spider.filter.robotstxt.IgnoredRobotsTxt;
import com.myseotoolbox.crawler.spider.filter.robotstxt.RobotsTxt;

import java.io.IOException;
import java.net.URI;

public class RobotsTxtBuilder {
    private static final HTTPClient httpClient = new HTTPClient();

    public static RobotsTxt buildRobotsTxtForOrigin(URI origin, boolean ignoreRobots) {
        try {
            String content = httpClient.get(origin.resolve("/robots.txt"));
            if (ignoreRobots) {
                return new IgnoredRobotsTxt(origin.toString(), content.getBytes());
            } else {
                return new DefaultRobotsTxt(origin.toString(), content.getBytes());
            }
        } catch (IOException e) {
            return new EmptyRobotsTxt(origin);
        }
    }
}

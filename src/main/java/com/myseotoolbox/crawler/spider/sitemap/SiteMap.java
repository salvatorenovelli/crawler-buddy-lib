package com.myseotoolbox.crawler.spider.sitemap;

import com.myseotoolbox.crawler.spider.filter.PathFilter;
import com.myseotoolbox.crawlercommons.UriCreator;
import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.UnknownFormatException;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.isHostMatching;


@Log4j2
public class SiteMap {

    private final URI origin;
    private final PathFilter pathFilter;
    private final List<URL> siteMaps;

    private SiteMapParser siteMapParser = new SiteMapParser(false);

    SiteMap(URI origin, String sitemapUrl) {
        this(origin, Collections.singletonList(sitemapUrl), Collections.singletonList("/"));
    }

    public SiteMap(URI origin, List<String> sitemaps, List<String> allowedPaths) {
        this.origin = origin;
        this.siteMaps = sitemaps.stream().map(this::mapToUrlOrLogWarning).filter(Objects::nonNull).collect(Collectors.toList());
        this.pathFilter = new PathFilter(allowedPaths);
    }

    public List<String> fetchUris() {
        return this.siteMaps
                .stream()
                .flatMap(url -> fetch(url).stream())
                .filter(this::isSameDomain)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Recursively scan Sitemap Index (the type of sitemap that has links to other sitemaps) and collect urls
     */
    private List<String> fetch(URL url) {

        if (!shouldFetch(url)) {
            return Collections.emptyList();
        }

        try {
            log.debug("Fetching sitemap on {}", url.toString());
            AbstractSiteMap asm = siteMapParser.parseSiteMap(url);
            if (asm instanceof SiteMapIndex) {
                SiteMapIndex smi = (SiteMapIndex) asm;
                return smi.getSitemaps().stream().flatMap(sm -> fetch(sm.getUrl()).stream()).distinct().collect(Collectors.toList());
            } else {
                crawlercommons.sitemaps.SiteMap sm = (crawlercommons.sitemaps.SiteMap) asm;
                return sm.getSiteMapUrls().stream().map(siteMapURL -> siteMapURL.getUrl().toString()).distinct().collect(Collectors.toList());
            }
        } catch (UnknownFormatException | IOException e) {
            log.warn("Error while fetching sitemap for {}. Error: {}", url, e.toString());
            return Collections.emptyList();
        }


    }

    private boolean shouldFetch(URL url) {
        try {
            return isSameDomain(url) && (this.siteMaps.contains(url) || pathFilter.shouldCrawl(URI.create(url.toString()), URI.create(url.toString())));
        } catch (IllegalArgumentException e) {
            log.warn("Unable to fetch sitemap on {}. {}", url, e.toString());
            return false;
        }
    }

    private boolean isSameDomain(String url) {
        return isHostMatching(UriCreator.create(url), origin, false);
    }

    private boolean isSameDomain(URL url) {
        return isSameDomain(url.toString());
    }

    private URL mapToUrlOrLogWarning(String s) {
        return Try.of(() -> new URL(s)).onFailure(throwable -> log.warn("Unable to crawl sitemap on {}. Error: {}", s, throwable.toString())).getOrElse((URL) null);
    }
}

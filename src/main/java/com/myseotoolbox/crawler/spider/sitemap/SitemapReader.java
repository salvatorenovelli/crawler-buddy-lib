package com.myseotoolbox.crawler.spider.sitemap;

import com.myseotoolbox.crawler.spider.UriFilter;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class SitemapReader {
    /*
     * There are sitemaps with millions of entries.
     * allowedPaths make sure we only fetch the sitemap indexes we need.
     * */
    public List<URI> getSeedsFromSitemaps(URI origin, List<String> sitemapsUrl, UriFilter filter) {
        log.info("Fetching {} sitemap for {} with filter: {}. Urls: {}", sitemapsUrl.size(), origin, filter, sitemapsUrl);
        List<URI> sitemapSeeds = new SiteMap(origin, sitemapsUrl, filter)
                .fetchUris()
                .stream()
                .map(this::toValidUri)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        log.info("Found {} seeds from sitemap for {}", sitemapSeeds.size(), origin);
        log.debug("{} seeds: {}", origin, sitemapSeeds);
        return sitemapSeeds;
    }

    private Optional<URI> toValidUri(String s) {
        try {
            return Optional.of(URI.create(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

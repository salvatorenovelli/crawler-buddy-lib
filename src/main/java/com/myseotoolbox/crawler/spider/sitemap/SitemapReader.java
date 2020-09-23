package com.myseotoolbox.crawler.spider.sitemap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SitemapReader {
    /*
     * There are sitemaps with millions of entries.
     * allowedPaths make sure we only fetch the sitemap indexes we need.
     * */
    public List<URI> getSeedsFromSitemaps(URI origin, List<String> sitemapsUrl, List<String> allowedPaths) {
        log.info("Fetching {} sitemap for {} with allowed paths: {}. Urls: {}", sitemapsUrl.size(), origin, allowedPaths, sitemapsUrl);
        List<URI> sitemapSeeds = new SiteMap(origin, sitemapsUrl, allowedPaths)
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

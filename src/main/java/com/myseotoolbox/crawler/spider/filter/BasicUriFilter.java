package com.myseotoolbox.crawler.spider.filter;

import com.myseotoolbox.crawler.spider.UriFilter;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.regex.Pattern;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.*;

@Log4j2
public class BasicUriFilter implements UriFilter {
    private static final Pattern INVALID_EXTENSIONS = Pattern.compile(".*\\.(?:css|js|bmp|gif|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz)$");
    private static final Pattern VALID_SCHEME = Pattern.compile("(?:http|https)$");


    private final URI websiteOrigin;

    public BasicUriFilter(URI websiteOrigin) {
        this.websiteOrigin = websiteOrigin;

    }

    @Override
    public boolean shouldCrawl(URI sourceUri, URI discoveredLink) {

        boolean valid = validScheme(discoveredLink) && validExtension(discoveredLink) && validHost(sourceUri, discoveredLink);

        if (!valid && log.isDebugEnabled()) {
            log.debug("Blocked: {} origin:{} sourceUri: {} discoveredLink: {}",
                    getFilterCause(validScheme(discoveredLink), validExtension(discoveredLink), validHost(sourceUri, discoveredLink)),
                    websiteOrigin,
                    sourceUri,
                    discoveredLink);
        }

        return valid;

    }

    private String getFilterCause(boolean scheme, boolean extension, boolean host) {
        return (!scheme ? "SCH " : "") + (!extension ? "EXT " : "") + (!host ? "HOST " : "");
    }

    private boolean validExtension(URI str) {
        return !INVALID_EXTENSIONS.matcher(str.toString().toLowerCase()).matches();
    }

    private boolean validHost(URI sourceUri, URI discoveredLink) {
        return isHostMatching(websiteOrigin, discoveredLink, false) ||
                isHostMatching(websiteOrigin, sourceUri, true) && areSubdomainsWithOrigin(discoveredLink);
    }

    private boolean areSubdomainsWithOrigin(URI discoveredLink) {
        return areSubdomains(websiteOrigin, discoveredLink, true);
    }

    private boolean validScheme(URI discoveredLink) {
        String scheme = discoveredLink.getScheme();
        return scheme != null && VALID_SCHEME.matcher(scheme).matches();
    }
}

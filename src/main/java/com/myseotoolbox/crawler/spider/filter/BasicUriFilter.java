package com.myseotoolbox.crawler.spider.filter;

import com.myseotoolbox.crawler.spider.UriFilter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.regex.Pattern;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.*;

@Slf4j
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
            log.debug("Blocked: {} URI: {} src: {}",
                    getFilterCause(validScheme(discoveredLink), validExtension(discoveredLink), validHost(sourceUri, discoveredLink)),
                    discoveredLink,
                    sourceUri);
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
        return (isChildOfOrigin(sourceUri) && isSubdomainOfOrigin(discoveredLink)) || isHostMatching(discoveredLink, websiteOrigin);
    }

    private boolean isSubdomainOfOrigin(URI discoveredLink) {
        return isSubdomain(websiteOrigin, discoveredLink);
    }

    private boolean isChildOfOrigin(URI discoveredLink) {
        return isChildOf(websiteOrigin, discoveredLink);
    }

    private boolean validScheme(URI discoveredLink) {
        String scheme = discoveredLink.getScheme();
        return scheme != null && VALID_SCHEME.matcher(scheme).matches();
    }
}

package com.myseotoolbox.crawler.spider;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.myseotoolbox.crawler.pagelinks.PageLink;
import com.myseotoolbox.crawler.utils.RemoveUrlFragment;
import com.myseotoolbox.crawler.utils.UrlDecoder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.myseotoolbox.crawler.utils.UriUtils.isValidUri;


@Log4j2
public class PageLinksHelper {

    public static final int MAX_URL_LEN = 1000;
    private static final Escaper ESCAPER = UrlEscapers.urlFragmentEscaper();

    public static List<PageLink> filterFollowablePageLinks(List<PageLink> links) {
        return links.stream().filter(PageLinksHelper::isFollowable).collect(Collectors.toList());
    }

    public static List<URI> filterValidPageLinks(List<PageLink> links) {

        if (links == null) return Collections.emptyList();

        List<String> followableLinks = links.stream()
                .filter(PageLinksHelper::isFollowable)
                .map(PageLink::getDestination)
                .collect(Collectors.toList());

        return filterValidUrls(followableLinks);
    }

    public static List<URI> filterValidUrls(List<String> urls) {
        List<URI> filtered = new ArrayList<>();

        if (urls != null) {
            filtered = urls
                    .stream()
                    .map(PageLinksHelper::toValidUri)
                    .flatMap(PageLinksHelper::stream)
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    private static <T> Stream<T> stream(Optional<T> opt) {
        return opt.map(Stream::of).orElseGet(Stream::empty);
    }

    private static boolean isFollowable(PageLink pageLink) {
        Map<String, String> attributes = pageLink.getAttributes();
        if (attributes != null && "nofollow".equals(attributes.get("rel"))) return false;

        return true;
    }

    public static Optional<URI> toValidUri(String str) {

        if (!isValidUri(str) || str.length() > MAX_URL_LEN) {
            return Optional.empty();
        }

        str = RemoveUrlFragment.removeFragment(str);
        str = str.trim();

        try {
            String decoded = UrlDecoder.decode(str);
            String transCoded = ESCAPER.escape(decoded);

            URI uri = new URI(transCoded);
            if (isEmptyLink(uri)) return Optional.empty();

            return Optional.of(uri);
        } catch (URISyntaxException  e) {
            log.debug("Invalid link: '{}'. {}", str, e.getMessage());
            return Optional.empty();
        }


    }

    private static boolean isEmptyLink(@Nullable URI uri) {
        if (uri == null) return false;
        try {
            String s = URLDecoder.decode(uri.toString(), "UTF-8");
            s = s.trim();
            return s.length() == 0;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

}

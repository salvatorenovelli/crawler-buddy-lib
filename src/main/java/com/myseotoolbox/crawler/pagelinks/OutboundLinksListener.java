package com.myseotoolbox.crawler.pagelinks;

import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.utils.UriUtils;
import org.bson.types.ObjectId;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.myseotoolbox.crawler.spider.filter.WebsiteOriginUtils.isHostMatching;
import static com.myseotoolbox.crawler.utils.IsCanonicalized.isCanonicalizedToDifferentUri;


public class OutboundLinksListener implements Consumer<CrawlResult> {
    private final ObjectId crawlId;
    private final OutboundLinkRepository repository;

    public OutboundLinksListener(ObjectId crawlId, OutboundLinkRepository repository) {
        this.crawlId = crawlId;
        this.repository = repository;
    }

    @Override
    public void accept(CrawlResult crawlResult) {

        if (isCanonicalizedToDifferentUri(crawlResult.getPageSnapshot())) return;

        HashMap<LinkType, List<String>> linkTypeListHashMap = new HashMap<>();
        linkTypeListHashMap.put(LinkType.AHREF, getLinks(crawlResult));

        repository.save(new OutboundLinks(null, crawlId, crawlResult.getUri(), URI.create(crawlResult.getUri()).getHost(), linkTypeListHashMap));
    }

    private List<String> getLinks(CrawlResult crawlResult) {
        List<String> links = crawlResult.getPageSnapshot().getLinks();
        if (links == null) return Collections.emptyList();
        return links.stream()
                .map(this::removeFragment)
                .filter(this::isValidUrl)
                .map(link -> relativize(crawlResult.getUri(), link))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String relativize(String pageUrl, String link) {

        try {

            URI pageUrlUri = new URI(pageUrl);
            URI linkUri = new URI(link);

            if (!linkUri.isAbsolute() || schemeMatching(pageUrlUri, linkUri) && isHostMatching(pageUrlUri, linkUri)) {
                return pageUrlUri.resolve(linkUri).getPath();
            }

        } catch (URISyntaxException e) {
            //nothing to do here, just an invalid link, will return original one.
        }

        return link;
    }

    private boolean schemeMatching(URI pageUrlUri, URI linkUri) {
        return Objects.equals(pageUrlUri.getScheme(), linkUri.getScheme());
    }

    private boolean isValidUrl(String url) {
        return UriUtils.isValidUri(url);
    }

    private String removeFragment(String url) {
        if (url.equals("#")) return "";
        if (!url.contains("#")) return url;
        return url.split("#")[0];
    }
}

package com.myseotoolbox.crawler.httpclient;

import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.model.RedirectChainElement;
import com.myseotoolbox.crawler.pagelinks.PageLink;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


class PageSnapshotBuilder {

    static PageSnapshot build(String uri, List<RedirectChainElement> elements, Document page) {

        PageSnapshot pageSnapshot = new PageSnapshot(
                removeFragment(uri),
                getTitle(page),
                getTagContents(page, "H1"),
                getTagContents(page, "H2"),
                extractFromTag(page, "meta[name=\"description\"]", element -> element.attr("content")),
                extractFromTag(page, "link[rel=\"canonical\"]", element -> element.attr("href"))
        );

        pageSnapshot.setLinks(extractPageLinks(page));
        pageSnapshot.setRedirectChainElements(elements);

        return pageSnapshot;
    }

    private static String getTitle(Document page) {
        return page.title().isBlank() ? getFirstTagContents(page, "TITLE") : page.title();
    }

    private static String removeFragment(String uri) {
        return uri.split("#")[0];
    }

    private static String getFirstTagContents(Element page, String tag) {
        return extractFromTag(page, tag, Element::html).stream().findFirst().orElse("");
    }

    private static List<String> getTagContents(Element page, String tag) {
        return extractFromTag(page, tag, Element::html);
    }

    private static List<PageLink> extractPageLinks(Document page) {
        return page.select("a[href]")
                .stream()
                .map(e -> new PageLink(e.attr("href"), extractLinkAdditionalAttributes(e)))
                .collect(Collectors.toList());
    }

    private static Map<String, String> extractLinkAdditionalAttributes(Element link) {
        return link.attributes().asList().stream()
                .filter(attribute -> attribute.getKey().equals("rel"))
                .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
    }

    private static List<String> extractFromTag(Element element, String filter, Function<Element, String> mapper) {
        return element
                .select(filter).stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
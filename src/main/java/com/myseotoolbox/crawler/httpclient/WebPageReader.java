package com.myseotoolbox.crawler.httpclient;

import com.myseotoolbox.crawler.CalendarService;
import com.myseotoolbox.crawler.model.*;
import com.myseotoolbox.crawler.spider.UriFilter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.UnsupportedMimeTypeException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static com.myseotoolbox.crawler.utils.IsRedirect.isRedirect;

@Log4j2
public class WebPageReader {

    private static final Pattern allowedContentTypeRegex = Pattern.compile("(application|text)/\\w*\\+?xml.*");

    private final HtmlParser parser = new HtmlParser();
    private final CalendarService calendarService = new CalendarService();
    private final UriFilter uriFilter;
    private final HttpRequestFactory httpRequestFactory;

    public WebPageReader(UriFilter uriFilter, HttpRequestFactory httpRequestFactory) {
        this.uriFilter = uriFilter;
        this.httpRequestFactory = httpRequestFactory;
    }

    public CrawlResult snapshotPage(URI uri) throws SnapshotException {

        String startURI = uri.toString();
        RedirectChain chain = new RedirectChain();

        try {

            URI baseUri = buildUri(startURI);

            if (scanRedirectChain(chain, baseUri)) {
                PageSnapshot snapshot = parser.parse(startURI, chain.getElements(), chain.getInputStream());
                snapshot.setCreateDate(calendarService.now());
                return CrawlResult.forSnapshot(snapshot);
            }

            return CrawlResult.forBlockedChain( chain);


        } catch (Exception e) {
            PageSnapshot pageSnapshot = new PageSnapshot();
            pageSnapshot.setUri(startURI);
            pageSnapshot.setCreateDate(calendarService.now());
            pageSnapshot.setRedirectChainElements(chain.getElements());
            pageSnapshot.setCrawlStatus("Unable to crawl: " + e);
            throw new SnapshotException(e, pageSnapshot);
        }

    }

    private boolean scanRedirectChain(RedirectChain redirectChain, URI currentURI) throws IOException, URISyntaxException, RedirectLoopException {

        HttpResponse response = httpRequestFactory.buildGetFor(currentURI).execute();

        int httpStatus = response.getHttpStatus();
        URI location = response.getLocation();

        redirectChain.addElement(new RedirectChainElement(currentURI.toString(), httpStatus, location.toString()));

        checkMimeType(response.getContentType(), currentURI);

        if (isRedirect(httpStatus)) {
            if (isBlockedChain(currentURI, location)) return false;
            return scanRedirectChain(redirectChain, location);
        } else {
            redirectChain.setInputStream(response.getInputStream());
            return true;
        }
    }

    private void checkMimeType(String contentType, URI url) throws UnsupportedMimeTypeException {
        if (contentType != null && !contentType.startsWith("text/") && !allowedContentTypeRegex.matcher(contentType).matches())
            throw new UnsupportedMimeTypeException("Unhandled content type. Must be text/*, application/xml, or application/xhtml+xml", contentType, url.toString());
    }

    private boolean isBlockedChain(URI currentURI, URI location) {
        return !uriFilter.shouldCrawl(currentURI, location);
    }

    private URI buildUri(String startURI) throws URISyntaxException {

        if (!startURI.startsWith("http")) {
            startURI = "http://" + startURI;
        }

        return new URI(startURI.trim());
    }

}
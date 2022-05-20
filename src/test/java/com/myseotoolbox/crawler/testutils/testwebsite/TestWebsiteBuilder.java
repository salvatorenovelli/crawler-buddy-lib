package com.myseotoolbox.crawler.testutils.testwebsite;

import com.myseotoolbox.crawler.httpclient.SafeStringEscaper;
import com.myseotoolbox.crawler.spider.PageLinksHelper;
import com.myseotoolbox.crawler.testutils.TestWebsite;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

@Log4j2
public class TestWebsiteBuilder {

    private final Server server;
    private Page curPage = null;
    private Map<String, Page> pages = new HashMap<>();

    static {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "INFO");
    }

    private TestWebsiteRequestHandler handler;

    InputStream robotsTxtStream;
    boolean robotsTxtRedirect = false;
    private Map<String, TestSiteMap> sitemaps = new HashMap<>();

    private TestWebsiteBuilder(Server server) {
        this.server = server;
    }

    public static TestWebsiteBuilder build() {
        return new TestWebsiteBuilder(new Server(0));
    }

    public TestWebsiteBuilder havingRootPage() {
        curPage = new Page("/");
        return this;
    }

    public TestWebsiteBuilder havingPage(String pagePath) {
        if (curPage != null) throw new IllegalStateException("You didn't save the previous stubbed page!");
        curPage = new Page(pagePath);
        return this;
    }

    public TestWebsiteBuilder redirectingTo(int status, String dstUri) {
        curPage.setAsRedirect(status, dstUri);
        return this;
    }

    public TestWebsiteBuilder withTitle(String s) {
        curPage.setTitle(s);
        return this;
    }

    public TestWebsiteBuilder withTag(String tagName, String content) {
        curPage.addTag(tagName, content);
        return this;
    }

    public TestWebsiteBuilder withMetaDescription(String s) {
        curPage.setMetaDescription(s);
        return this;
    }

    public TestWebsiteBuilder withMimeType(String mimeType) {
        this.curPage.setMimeType(mimeType);
        return this;
    }

    public TestWebsiteBuilder withLinksTo(String... links) {
        this.curPage.addLinks(Arrays.asList(links), "");
        return this;
    }

    public TestWebsiteBuilder withNoFollowLinksTo(String... links) {
        this.curPage.addLinks(Arrays.asList(links), "rel=\"nofollow\"");
        return this;
    }

    public TestWebsiteBuilder and() {
        if (curPage != null) {
            addPage(curPage);
            curPage = null;
        }
        return this;
    }

    public URI buildTestUri(String path) {
        return buildTestUri("", path);
    }

    public URI buildTestUri(String subDomain, String pathStr) {

        if (!server.isStarted()) {
            throw new IllegalStateException("Sorry, you'll need to start the server before asking for URI. (At the moment the server port is not known)");
        }
        Optional<URI> path = PageLinksHelper.toValidUri(pathStr);
        return URI.create(getBaseUriAsString(subDomain)).resolve(path.get());
    }

    public String getBaseUriAsString() {
        return getBaseUriAsString("");
    }

    public String getBaseUriAsString(String subDomain) {
        int localPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        String possibleSubDomain = subDomain.isEmpty() ? "" : subDomain + ".";
        return "http://" + possibleSubDomain + "localhost:" + localPort;
    }

    public TestWebsite save() {
        if (curPage != null) {
            addPage(curPage);
        }
        return handler;
    }

    public TestWebsite run() throws Exception {

        and();
        handler = new TestWebsiteRequestHandler(this);
        server.setHandler(handler);
        server.start();
        log.info("Test server listening on http://localhost:{}", ((ServerConnector) server.getConnectors()[0]).getLocalPort());

        return handler;
    }

    public void tearDown() throws Exception {
        server.stop();
    }

    Page getPage(String s) {
        return pages.get(encode(s));
    }

    private String encode(String s) {
        return SafeStringEscaper.escapeString(s);
    }

    private void addPage(Page page) {
        pages.putIfAbsent(encode(page.getPagePath()), page);
    }

    public TestWebsiteBuilder disableCharsetHeader() {
        curPage.setCharsetFieldPresent(false);
        return this;
    }

    public TestRobotsTxtBuilder withRobotsTxt() {
        return new TestRobotsTxtBuilder(this);
    }

    public TestWebsiteBuilder withRobotsTxt(InputStream stream) {
        this.robotsTxtStream = stream;
        return this;
    }

    public TestWebsiteBuilder withRobotTxtHavingRedirect() {
        this.robotsTxtRedirect = true;
        return this;
    }

    public TestSiteMapBuilder withSitemapOn(String location) {
        return new TestSiteMapBuilder(this, location, false);
    }

    public TestSiteMap getSitemap(String path) {
        return sitemaps.get(path);
    }

    public TestSiteMapBuilder withSitemapIndexOn(String location) {
        return new TestSiteMapBuilder(this, location, true);
    }

    public class TestSiteMapBuilder {
        private final TestWebsiteBuilder parent;
        private final String location;
        private final boolean isSitemapIndex;
        private List<String> urls = new ArrayList<>();

        public TestSiteMapBuilder(TestWebsiteBuilder parent, String location, boolean isSitemapIndex) {
            this.parent = parent;
            this.location = location + (location.endsWith("xml") ? "" : "sitemap.xml");
            this.isSitemapIndex = isSitemapIndex;
        }

        public TestSiteMapBuilder havingUrls(String... urls) {
            this.urls.addAll(Arrays.asList(urls));
            return this;
        }

        public TestSiteMapBuilder havingChildSitemaps(String... childUrls) {
            this.urls.addAll(Arrays.asList(childUrls));
            return this;
        }

        public TestWebsiteBuilder build() {
            this.parent.addSitemap(location, new TestSiteMap(isSitemapIndex, urls));
            return parent;
        }

        public TestWebsiteBuilder and() {
            return build();
        }
    }

    private void addSitemap(String location, TestSiteMap sitemap) {
        this.sitemaps.put(location, sitemap);
    }

    @Getter
    public class TestSiteMap {
        private final boolean isSiteMapIndex;
        private final List<String> urls;

        public TestSiteMap(boolean isSiteMapIndex, List<String> urls) {
            this.isSiteMapIndex = isSiteMapIndex;
            this.urls = urls;
        }
    }
}


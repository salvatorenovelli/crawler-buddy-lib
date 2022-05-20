package com.myseotoolbox.crawler.testutils.testwebsite;

import com.myseotoolbox.crawler.httpclient.SafeStringEscaper;
import com.myseotoolbox.crawler.testutils.TestWebsite;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.myseotoolbox.crawler.utils.IsRedirect.isRedirect;

@Log4j2
class TestWebsiteRequestHandler extends AbstractHandler implements TestWebsite {
    private TestWebsiteBuilder testWebsiteBuilder;
    private List<ReceivedRequest> requestsReceived = new ArrayList<>();

    public TestWebsiteRequestHandler(TestWebsiteBuilder testWebsiteBuilder) {
        this.testWebsiteBuilder = testWebsiteBuilder;
    }

    @Override
    public void handle(String s, Request request,
                       HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse) {


        requestsReceived.add(ReceivedRequest.from(request));
        log.info("Received request for path '{}'. {}", s, request);


        String path;

        // This is to compensate with Jetty "trying" to decode URI with the right charset. (The problem is that we need to emulate bad behaving server that decode with different charsets)
        if (SafeStringEscaper.containsUnicodeCharacters(s)) {
            path = s;
        } else {
            path = request.getHttpURI().getPath();
        }

        if (path.contains("robots.txt")) {
            serveRobotsTxt(request, httpServletResponse, path);
        } else if (path.contains("sitemap")) {
            serveSitemap(request, httpServletResponse, path);
        } else {
            String decoded = decode(path);
            Page page = testWebsiteBuilder.getPage(decoded);
            if (page != null) {
                servePage(page, httpServletResponse);
                request.setHandled(true);
            }
        }


    }

    @SneakyThrows
    private String decode(String path) {
        return URLDecoder.decode(path, "UTF-8");
    }

    private void serveSitemap(Request request, HttpServletResponse response, String path) {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("content-type", "application/xml");

        try (OutputStream outputStream = response.getOutputStream()) {
            CharSequence render = render(this.testWebsiteBuilder.getSitemap(path));
            IOUtils.write(render, outputStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        request.setHandled(true);
    }

    private CharSequence render(TestWebsiteBuilder.TestSiteMap siteMap) {
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");


        if (siteMap != null) {

            if (siteMap.isSiteMapIndex()) {
                renderSitemapIndex(sb, siteMap);
            } else {
                renderSitemap(sb, siteMap);
            }
        }


        return sb;
    }

    private void renderSitemapIndex(StringBuffer sb, TestWebsiteBuilder.TestSiteMap siteMap) {
        sb.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (String url : siteMap.getUrls()) {

            String out = testWebsiteBuilder.getBaseUriAsString() + url + "sitemap.xml";
            if (url.startsWith("http")) {
                out = url;
            }

            sb.append("<sitemap><loc>" + out + "</loc></sitemap>\n");

        }

        sb.append("</sitemapindex>");
    }

    private void renderSitemap(StringBuffer sb, TestWebsiteBuilder.TestSiteMap siteMap) {
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (String url : siteMap.getUrls()) {
            String out = testWebsiteBuilder.getBaseUriAsString() + url;
            if (url.startsWith("http")) {
                out = url;
            }
            sb.append("<url><loc>").append(out).append("</loc></url>\n");
        }

        sb.append("</urlset>");
    }

    private void serveRobotsTxt(Request request, HttpServletResponse httpServletResponse, String path) {

        if (testWebsiteBuilder.robotsTxtStream == null) return;

        if (path.equals("/robots.txt") && testWebsiteBuilder.robotsTxtRedirect) {
            httpServletResponse.setStatus(301);
            httpServletResponse.setHeader("location", "/other/robots.txt");
        } else {
            httpServletResponse.setStatus(200);
            try (OutputStream outputStream = httpServletResponse.getOutputStream()) {
                outputStream.write(IOUtils.toString(testWebsiteBuilder.robotsTxtStream, StandardCharsets.UTF_8).getBytes());
            } catch (IOException e) {
                log.warn("No robots txt configured", e);
            }
        }

        request.setHandled(true);
    }

    private void servePage(Page page, HttpServletResponse response) {
        int status = page.getStatus();

        if (status == 200) {
            serveStandard(page, response);
        } else if (isRedirect(status)) {
            serveAsRedirect(page, response);
        } else {
            throw new UnsupportedOperationException("Status not supported" + status);
        }


    }

    private void serveAsRedirect(Page page, HttpServletResponse response) {
        response.setContentType("text/html;" + (page.isCharsetFieldPresent() ? "charset=UTF-8" : ""));
        response.setStatus(page.getStatus());
        response.setHeader("location", page.getRedirectUri());
    }

    private void serveStandard(Page page, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("content-type", page.getMimeType());

        try (OutputStream outputStream = response.getOutputStream()) {
            IOUtils.write(renderPage(page), outputStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CharSequence renderPage(Page page) {
        StringBuffer sb = new StringBuffer();
        sb.append("<HTML>");
        sb.append("    <HEAD>");
        sb.append("        <TITLE>").append(page.getTitle()).append("</TITLE>");
        sb.append("        <META name=\"description\" content=\"").append(page.getMetaDescription()).append("\" />");
        sb.append("    </HEAD>");
        sb.append("    <BODY>");
        addTags(sb, page);
        addLinks(sb, page);
        sb.append("    </BODY>");
        sb.append("</HTML>");
        return sb;

    }

    private void addLinks(StringBuffer sb, Page page) {
        for (Link link : page.getLinks()) {
            sb.append("        <a href='").append(link.getUrl()).append("'")
                    .append(" ").append(link.getAttributes()).append(" ")
                    .append(">link</a>");
        }
    }

    private void addTags(StringBuffer sb, Page page) {
        for (String tag : page.getTags().keySet()) {
            for (String content : page.getTags().get(tag)) {
                sb.append("        <").append(tag).append(">").append(content).append("</").append(tag).append(">");
            }
        }
    }

    @Override
    public List<ReceivedRequest> getRequestsReceived() {
        return requestsReceived;
    }
}

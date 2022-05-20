package com.myseotoolbox.crawler.httpclient;

import com.myseotoolbox.crawler.MetaTagSanitizer;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.model.RedirectChainElement;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
@Log4j2
public class HtmlParser {
    public PageSnapshot parse(String baseUri, List<RedirectChainElement> elements, InputStream is) throws IOException {
        Document document = Jsoup.parse(is, UTF_8.name(), baseUri);
        PageSnapshot snapshot = PageSnapshotBuilder.build(baseUri, elements, document);
        MetaTagSanitizer.sanitize(snapshot);
        return snapshot;
    }
}

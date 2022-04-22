package com.myseotoolbox.crawler.model;

import com.myseotoolbox.crawler.websitecrawl.WebsiteCrawl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CrawlCompletedEvent {
    private SimpleWebsiteCrawl websiteCrawl;

    public CrawlCompletedEvent(WebsiteCrawl websiteCrawl) {
        this.websiteCrawl = new SimpleWebsiteCrawl(websiteCrawl.getId(), websiteCrawl.getOrigin());
    }
}

@AllArgsConstructor
@NoArgsConstructor
@Data
class SimpleWebsiteCrawl {
    private String id;
    private String origin;
}
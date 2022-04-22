package com.myseotoolbox.crawler.model;


import com.myseotoolbox.crawler.pagelinks.PageLink;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Getter
@NoArgsConstructor
@Setter
@EqualsAndHashCode
public class PageSnapshot {

    private String id;
    private String uri;
    private Date createDate;

    private List<RedirectChainElement> redirectChainElements;

    private String title;
    private List<String> h1s;
    private List<String> h2s;
    private List<String> metaDescriptions;
    private List<String> canonicals;

    private List<PageLink> links;

    private String crawlStatus;

    public PageSnapshot(String uri, String title, List<String> h1s, List<String> h2s, List<String> metaDescriptions, List<String> canonicals) {
        this.uri = uri;
        this.title = title;
        this.h1s = h1s;
        this.h2s = h2s;
        this.metaDescriptions = metaDescriptions;
        this.canonicals = canonicals;
    }

    @Override
    public String toString() {
        return "PageSnapshot{" +
                "id='" + id + '\'' +
                ", createDate=" + createDate +
                ", uri='" + uri + '\'' +
                ", title='" + title + '\'' +
                ", h1s=" + limitLen(h1s) +
                ", h2s=" + limitLen(h2s) +
                ", metaDescriptions=" + limitLen(metaDescriptions) +
                ", canonicals=" + limitLen(canonicals) +
                ", rce=" + redirectChainElements +
                ", crawlStatus='" + crawlStatus + '\'' +
                '}';
    }

    private List<String> limitLen(List<String> str) {
        if (str == null) return null;
        return str.stream()
                .filter(Objects::nonNull)
                .map(s -> s.substring(0, Math.min(s.length(), 20)))
                .collect(toList());
    }
}

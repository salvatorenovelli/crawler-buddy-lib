package com.myseotoolbox.crawler.pagelinks;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class OutboundLinks {
    private String id;
    private final String crawlId;
    private final String url;
    private final LocalDateTime crawledAt;
    private final String domain;
    private final Map<LinkType, List<String>> linksByType;
}

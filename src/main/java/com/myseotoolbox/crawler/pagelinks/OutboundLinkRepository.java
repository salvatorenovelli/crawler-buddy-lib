package com.myseotoolbox.crawler.pagelinks;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OutboundLinkRepository extends MongoRepository<OutboundLinks, String> {
    Optional<OutboundLinks> findByCrawlIdAndUrl(String crawlId, String url);
}
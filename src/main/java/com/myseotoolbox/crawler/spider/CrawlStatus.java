package com.myseotoolbox.crawler.spider;

import lombok.extern.log4j.Log4j2;

import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Log4j2
@ThreadSafe
class CrawlStatus {
    private final Set<String> visited = new HashSet<>();
    private final Set<String> inProgress = new HashSet<>();

    public synchronized void addToInProgress(List<URI> uris) {
        log.debug("Adding to in progress: {}", uris);
        inProgress.addAll(toString(uris));
    }

    public synchronized void markAsCrawled(URI uri) {
        log.debug("Marking as crawled: {}", uri);
        if (!inProgress.remove(toString(uri))) {
            throw new IllegalStateException("Completing snapshot of not in progress URI: '" + uri + "' Visited: " + visited.contains(toString(uri)));
        }
        if (!visited.add(toString(uri)))
            throw new IllegalStateException("Already visited: " + uri);
    }

    public synchronized boolean isAlreadyVisited(URI uri) {
        return visited.contains(toString(uri)) || inProgress.contains(toString(uri));
    }

    public synchronized int getTotalEnqueued() {
        return inProgress.size() + visited.size();
    }

    public synchronized boolean isCrawlCompleted() {
        return inProgress.size() == 0;
    }

    private String toString(URI uri) {
        return uri.toASCIIString();
    }

    private Collection<String> toString(Collection<URI> uris) {
        return uris.stream().map(this::toString).collect(Collectors.toSet());
    }
}

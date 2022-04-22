//package com.myseotoolbox.crawler.testutils;
//
//
//import com.myseotoolbox.crawler.model.*;
//import com.myseotoolbox.crawler.repository.PageSnapshotRepository;
//
//import java.net.URI;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri;
//import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.buildRedirectChainElementsFor;
//
//
//public class MonitoredUriBuilder {
//
//    public static final String DEFAULT_USER = "default_user" ;
//    public static final int TEST_WORKSPACE_NUMBER = 19514; //just a random number to avoid hardcoded stuff
//    public static final String WEBSITE_CRAWL_ID = "2340239845nn" ;
//    private static MonitoredUriRepository monitoredUriRepo;
//    private static PageSnapshotRepository pageSnapshotRepo;
//
//    private static final AtomicInteger i = new AtomicInteger(0);
//    private final MonitoredUri monitoredUri;
//    private final String uri;
//
//    private MonitoredUriBuilder() {
//        int id = i.getAndIncrement();
//        this.uri = "http://monitored-uri-builder-test-uri" + id;
//        Recommendation recomm = RecommendationTestBuilder.aRecommendation()
//                .withTitle("Title" + id)
//                .withH1s(id + "H1-1", id + "H1-2")
//                .withMetas(id + "Meta1", id + "Meta2").build();
//        LastCrawl lastCrawl = new LastCrawl(WEBSITE_CRAWL_ID);
//        lastCrawl.setInboundLinksCount(new InboundLinksCount());
//        this.monitoredUri = new MonitoredUri(null, uri, DEFAULT_USER, TEST_WORKSPACE_NUMBER, recomm, null, lastCrawl, "");
//    }
//
//    public static void setUp(MonitoredUriRepository monitoredUriRepo, PageSnapshotRepository pageSnapshotRepo) {
//        MonitoredUriBuilder.monitoredUriRepo = monitoredUriRepo;
//        MonitoredUriBuilder.pageSnapshotRepo = pageSnapshotRepo;
//    }
//
//    public static MonitoredUriBuilder givenAMonitoredUri() {
//        return new MonitoredUriBuilder();
//    }
//
//
//    public static MonitoredUri givenTestMonitoredUriForUser(String uri, String user) {
//        return givenAMonitoredUri().forUser(user).forUri(uri).save();
//    }
//
//    public static MonitoredUri givenTestMonitoredUri(String uri) {
//        return givenAMonitoredUri().forUri(uri).save();
//    }
//
//    public static void tearDown() {
//        DataTestHelper.clearRepos(monitoredUriRepo, pageSnapshotRepo);
//    }
//
//    public MonitoredUriBuilder forUser(String user) {
//        monitoredUri.setOwnerName(user);
//        return this;
//    }
//
//
//    public MonitoredUri save() {
//        saveCurrentValueIfInitialized();
//        return monitoredUriRepo.save(monitoredUri);
//    }
//
//    public MonitoredUriBuilder forUri(String uri) {
//        monitoredUri.setUri(uri);
//        return this;
//    }
//
//    private void saveCurrentValueIfInitialized() {
//        PageSnapshot currentValue = monitoredUri.getCurrentValue();
//        if (currentValue != null) {
//            PageSnapshot savedCurrentValue = pageSnapshotRepo.save(currentValue);
//            monitoredUri.setCurrentValue(savedCurrentValue);
//        }
//    }
//
//    public MonitoredUri build() {
//        return monitoredUri;
//    }
//
//    public MonitoredUriBuilder.RecommendationBuilder withRecommendationHaving() {
//        return new RecommendationBuilder(this);
//    }
//
//    public MonitoredUriBuilder.CurrentValueBuilder withCurrentValue() {
//        return new CurrentValueBuilder(this);
//    }
//
//    public MonitoredUriBuilder havingInternalHrefLinksCount(int count) {
//        this.monitoredUri.getLastCrawl().getInboundLinksCount().setExternal(new InboundLinks(count, null, null));
//        return this;
//    }
//
//    public MonitoredUriBuilder havingExternalHrefLinksCount(int count) {
//        this.monitoredUri.getLastCrawl().getInboundLinksCount().setExternal(new InboundLinks(count, null, null));
//
//        return this;
//    }
//
//    public MonitoredUriBuilder withNoRecommendation() {
//        monitoredUri.setRecommendation(null);
//        return this;
//    }
//
//    public MonitoredUriBuilder withEmptyRecommendation() {
//        monitoredUri.setRecommendation(new Recommendation());
//        return this;
//    }
//
//    public MonitoredUriBuilder forWorkspace(int workspaceNumber) {
//        monitoredUri.setWorkspaceNumber(workspaceNumber);
//        return this;
//    }
//
//    public class RecommendationBuilder {
//
//        Recommendation recommendation = new Recommendation();
//        private final MonitoredUriBuilder monitoredUriBuilder;
//
//        public RecommendationBuilder(MonitoredUriBuilder monitoredUriBuilder) {
//            this.monitoredUriBuilder = monitoredUriBuilder;
//        }
//
//        public RecommendationBuilder title(String s) {
//            recommendation.setTitle(s);
//            return this;
//        }
//
//        public RecommendationBuilder h1(String... s) {
//            recommendation.setH1s(Arrays.asList(s));
//            return this;
//        }
//
//        public RecommendationBuilder h1s(List<String> s) {
//            recommendation.setH1s(s);
//            return this;
//        }
//
//
//        public RecommendationBuilder meta(String... s) {
//            recommendation.setMetaDescriptions(Arrays.asList(s));
//            return this;
//        }
//
//        public RecommendationBuilder destinationUri(String uri) {
//
//            //naive version of Uri.isAbsolute. It's necessary because we need to inject invalid uris here
//            if (uri.startsWith("http")) {
//                recommendation.setDestinationUri(uri);
//            } else {
//                recommendation.setDestinationUri(monitoredUri.getUri() + uri);
//            }
//
//
//            return this;
//        }
//
//
//        public MonitoredUriBuilder and() {
//            monitoredUriBuilder.monitoredUri.setRecommendation(recommendation);
//            return monitoredUriBuilder;
//        }
//
//        public MonitoredUri save() {
//            return and().save();
//        }
//    }
//
//    public class CurrentValueBuilder {
//
//        private final PageSnapshot curValue = aPageSnapshotWithStandardValuesForUri(uri);
//        private final MonitoredUriBuilder monitoredUriBuilder;
//
//        public CurrentValueBuilder(MonitoredUriBuilder monitoredUriBuilder) {
//            this.monitoredUriBuilder = monitoredUriBuilder;
//        }
//
//        public CurrentValueBuilder scanned(LocalDate localDate) {
//            this.curValue.setCreateDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
//            return this;
//        }
//
//        public CurrentValueBuilder title(String title) {
//            this.curValue.setTitle(title);
//            return this;
//        }
//
//        public CurrentValueBuilder meta(String... meta) {
//            this.curValue.setMetaDescriptions(Arrays.asList(meta));
//            return this;
//        }
//
//        public CurrentValueBuilder h1(String... h1) {
//            this.curValue.setH1s(Arrays.asList(h1));
//            return this;
//        }
//
//        public CurrentValueBuilder h2(String... h2) {
//            this.curValue.setH2s(Arrays.asList(h2));
//            return this;
//        }
//
//        public CurrentValueBuilder havingRedirectWithDestination(String relativeDstUri) {
//            String dstUri = URI.create(this.curValue.getUri()).resolve(relativeDstUri).toString();
//
//            this.curValue.setRedirectChainElements(Arrays.asList(
//                    new RedirectChainElement(this.curValue.getUri(), 301, dstUri),
//                    new RedirectChainElement(dstUri, 200, dstUri)
//            ));
//            return this;
//        }
//
//        public CurrentValueBuilder havingRedirectChain(int... statuses) {
//            List<RedirectChainElement> elements = buildRedirectChainElementsFor(this.curValue.getUri(), statuses);
//            this.curValue.setRedirectChainElements(elements);
//            return this;
//        }
//
//        public CurrentValueBuilder havingEmptyRedirectChain() {
//            this.curValue.setRedirectChainElements(Collections.emptyList());
//            return this;
//        }
//
//        public MonitoredUriBuilder and() {
//            monitoredUriBuilder.monitoredUri.setCurrentValue(curValue);
//            return monitoredUriBuilder;
//        }
//
//        public MonitoredUri save() {
//            return and().save();
//        }
//    }
//}
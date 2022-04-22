//package com.myseotoolbox.crawler.pagelinks;
//
//import com.myseotoolbox.crawler.model.CrawlResult;
//import com.myseotoolbox.crawler.model.PageSnapshot;
//import com.myseotoolbox.crawler.model.RedirectChainElement;
//import org.bson.types.ObjectId;
//import org.hamcrest.Matchers;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentMatchers;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.net.URI;
//import java.util.function.Consumer;
//
//import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aTestPageSnapshotForUri;
//import static java.util.Collections.singletonMap;
//import static org.hamcrest.Matchers.*;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertThat;
//
//
//@SuppressWarnings("CodeBlock2Expr")
//@RunWith(MockitoJUnitRunner.class)
//public class OutboundLinksPersistenceListenerTest {
//
//    private static final String TEST_ORIGIN = "http://domain";
//    private static final URI CRAWL_ORIGIN = URI.create(TEST_ORIGIN);
//    @Mock OutboundLinkRepository repository;
//    public static final ObjectId TEST_CRAWL_ID = new ObjectId();
//    private OutboundLinksPersistenceListener sut;
//
//    @Before
//    public void setUp() {
//        sut = new OutboundLinksPersistenceListener(TEST_CRAWL_ID, TEST_ORIGIN, repository);
//    }
//
//    @Test
//    public void shouldSaveDiscoveredLinks() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("/relativeLink", "http://absoluteLink/hello");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
//            assertThat(outboundLinks.getUrl(), equalTo(TEST_ORIGIN));
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/relativeLink", "http://absoluteLink/hello"));
//        });
//    }
//
//
//    @Test
//    public void shouldNotPersistDuplicateLinks() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("/relativeLink", "/relativeLink", "http://absoluteLink/hello", "http://absoluteLink/hello");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
//            assertThat(outboundLinks.getUrl(), equalTo("http://domain"));
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/relativeLink", "http://absoluteLink/hello"));
//        });
//    }
//
//    @Test
//    public void shouldBeFineWithNullLinks() {
//        CrawlResult crawlResult = CrawlResult.forSnapshot(aTestPageSnapshotForUri("http://testuri").withNullLinks().build());
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
//            assertThat(outboundLinks.getUrl(), equalTo("http://testuri"));
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), hasSize(0));
//        });
//    }
//
//    @Test
//    public void shouldSkipFailedCrawls() {
//        CrawlResult crawlResult = CrawlResult.forSnapshot(
//                aTestPageSnapshotForUri("http://testuri")
//                        .withEmptyRedirectChain()
//                        .build());
//
//        sut.accept(crawlResult);
//
//        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any(OutboundLinks.class));
//    }
//
//    @Test
//    public void shouldNotPersistFragments() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("#this-is-a-fragment", "http://absoluteLink/hello#fragment");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://absoluteLink/hello"));
//        });
//    }
//
//    @Test
//    public void shouldPersistParameters() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("http://domain/index?param=true", "http://domain/index?param=false", "http://domain/index");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/index?param=true", "/index?param=false", "/index"));
//        });
//    }
//
//    @Test
//    public void shouldPersistParametersForExternalDomains() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("http://externaldomain/index.php?param=true", "http://externaldomain/index.php?param=false");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://externaldomain/index.php?param=true", "http://externaldomain/index.php?param=false"));
//        });
//    }
//
//
//    @Test
//    public void shouldNotMergeLeadingSlashWithNon() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("http://domain/index", "http://domain/index/");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/index", "/index/"));
//        });
//    }
//
//    @Test
//    public void shouldPersistCanonicals() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithCanonicals("http://domain/it/canonical1", "http://domain/it/canonical2");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.CANONICAL), containsInAnyOrder("/it/canonical1", "/it/canonical2"));
//        });
//    }
//
//    @Test
//    public void shouldNotPersistNoFollow() {
//
//        PageLink notFollowable = new PageLink("/notFollowable", singletonMap("rel", "nofollow"));
//        PageLink followable = new PageLink("/followable", singletonMap("rel", "nex"));
//
//        CrawlResult crawlResult = givenCrawlResultForPageWithNoFollowLinks(notFollowable, followable);
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), hasSize(1));
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF).get(0), is("/followable"));
//        });
//    }
//
//    @Test
//    public void shouldDedupPagesWithFragments() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("http://host/hello", "http://host/hello#fragment");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://host/hello"));
//        });
//    }
//
//    @Test
//    public void canHandleEmptyFragments() {
//        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("#", "http://host/page#");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://host/page"));
//        });
//    }
//
//    @Test
//    public void shouldAlwaysPersistRelativeIfIsSameDomain() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
//                "/link1",
//                "http://domain/link2",
//                "http://domain/some/path/link3",
//                "http://anotherdomain/link1");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "/link2", "/some/path/link3", "http://anotherdomain/link1"));
//        });
//    }
//
//    @Test
//    public void shouldBeAbleToRelativizeUrlsWithSpaces() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
//                "http://domain/link1 with spaces/salve");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1%20with%20spaces/salve"));
//        });
//    }
//
//
//    @Test
//    public void shouldBeAbleToRelativizeUrlsUnicodeCharacters() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
//                "http://domain/linkWithUnicode\u200B  \u200B");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/linkWithUnicode%E2%80%8B%20%20%E2%80%8B"));
//        });
//    }
//
//    @Test
//    public void shouldRelativizeBasedOnOrigin() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("https://domain/some/path", "https://domain/link");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("https://domain/link"));
//        });
//    }
//
//    @Test
//    public void shouldSaveAbsoluteLinksIfNonSameDomainAsOrigin() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("https://domain/some/path", "/link");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("https://domain/link"));
//        });
//    }
//
//    @Test
//    public void shouldPersistLinksWithSpacesAtTheEnd() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain", "http://domain/path ");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/path"));
//        });
//    }
//
//    @Test
//    public void shouldRelativizeRelativeToCurrentUrls() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path/",
//                "salve");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/some/path/salve"));
//        });
//    }
//
//    @Test
//    public void differentSchemaIsConsideredDifferentHost() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
//                "/link1",
//                "https://domain/link2");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "https://domain/link2"));
//        });
//    }
//
//    @Test
//    public void linksWithNoSlashAtTheBeginningShouldBeResolvedProperly() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/subpath/page", "link1");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/subpath/link1"));
//        });
//    }
//
//    @Test
//    public void invalidUrlShouldBePersistedAsTheyAre() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
//                "http:-/link1__(this is invalid)__",
//                "https://domain/link2");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http:-/link1__(this%20is%20invalid)__", "https://domain/link2"));
//        });
//    }
//
//    @Test
//    public void shouldPersistDomain() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://something.domain/some/path", "/link1");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getDomain(), is("something.domain"));
//        });
//    }
//
//    @Test
//    public void shouldNotPersistJavascriptLinks() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
//                "/link1",
//                "javascript:void(0)");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1"));
//        });
//    }
//
//    @Test
//    public void linksShouldBeSorted() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
//                "/c", "/d", "/c", "/b", "http://domain/a", "http://anotherdomain/b", "http://anotherdomain/a");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), Matchers.contains("/a", "/b", "/c", "/d", "http://anotherdomain/a", "http://anotherdomain/b"));
//        });
//    }
//
//    @Test
//    public void shouldMaintainTrailingSlashIfPresent() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
//                "/link1/", "/link1");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "/link1/"));
//        });
//    }
//
//    @Test
//    public void shouldSaveCrawlDate() {
//        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
//                "/link1/", "/link1");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertNotNull(outboundLinks.getCrawledAt());
//        });
//    }
//
//    @Test
//    public void shouldResolveLinksBasedOnDestinationUrl() {
//        CrawlResult crawlResult = givenCrawlResultWithRedirect("http://domain", "https://domain", "/link1", "/link2");
//
//        sut.accept(crawlResult);
//
//        verifySavedLinks(outboundLinks -> {
//            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("https://domain/link1", "https://domain/link2"));
//        });
//    }
//
//    private void verifySavedLinks(Consumer<OutboundLinks> linksVerify) {
//        Mockito.verify(repository).save(ArgumentMatchers.argThat(argument -> {
//            linksVerify.accept(argument);
//            return true;
//        }));
//    }
//
//    private CrawlResult givenCrawlResultForPageWithCanonicals(String... canonicals) {
//        return CrawlResult.forSnapshot(aTestPageSnapshotForUri(TEST_ORIGIN)
//                .withRedirectChainElements(new RedirectChainElement(TEST_ORIGIN, 200, TEST_ORIGIN))
//                .withCanonicals(canonicals).build());
//    }
//
//    private CrawlResult givenCrawlResultForPageWithNoFollowLinks(PageLink... links) {
//        return CrawlResult.forSnapshot(
//                aTestPageSnapshotForUri(TEST_ORIGIN)
//                        .withRedirectChainElements(new RedirectChainElement(TEST_ORIGIN, 200, TEST_ORIGIN))
//                        .withLinks(links)
//                        .build()
//        );
//    }
//
//    private CrawlResult givenCrawlResultForPageWithLinks(String... links) {
//        return givenCrawlResultForUrlWithPageWithLinks(TEST_ORIGIN, links);
//    }
//
//    private CrawlResult givenCrawlResultWithRedirect(String url, String destinationUrl, String... links) {
//        PageSnapshot build = aTestPageSnapshotForUri(url)
//                .withRedirectChainElements(new RedirectChainElement(url, 301, destinationUrl), new RedirectChainElement(destinationUrl, 200, destinationUrl))
//                .withLinks(links).build();
//        return CrawlResult.forSnapshot(build);
//    }
//
//    private CrawlResult givenCrawlResultForUrlWithPageWithLinks(String url, String... links) {
//        return CrawlResult.forSnapshot(aTestPageSnapshotForUri(url)
//                .withRedirectChainElements(new RedirectChainElement(url, 200, url))
//                .withLinks(links).build());
//    }
//}
package com.myseotoolbox.crawler.pagelinks;

import com.myseotoolbox.crawler.model.CrawlResult;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Consumer;

import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aTestPageSnapshotForUri;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class OutboundLinksPersistenceListenerTest {

    @Mock OutboundLinkRepository repository;
    public static final ObjectId TEST_CRAWL_ID = new ObjectId();
    private OutboundLinksPersistenceListener sut;

    @Before
    public void setUp() {
        sut = new OutboundLinksPersistenceListener(TEST_CRAWL_ID, repository);
    }

    @Test
    public void shouldSaveDiscoveredLinks() {
        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("/relativeLink", "http://absoluteLink/hello");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
            assertThat(outboundLinks.getUrl(), equalTo("http://testuri"));
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/relativeLink", "http://absoluteLink/hello"));
        });
    }

    @Test
    public void shouldOnlyPersistSelfCanonicalized() {
        CrawlResult crawlResult = CrawlResult.forSnapshot(
                aTestPageSnapshotForUri("http://testuri?someWeirdDinamicUrl=2b234rb9b")
                        .withCanonicals("http://testuri")
                        .withLinks("/relativeLink", "http://absoluteLink/hello").build());

        sut.accept(crawlResult);

        Mockito.verifyNoMoreInteractions(repository);
    }

    @Test
    public void shouldNotPersistDuplicateLinks() {
        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("/relativeLink", "/relativeLink", "http://absoluteLink/hello", "http://absoluteLink/hello");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
            assertThat(outboundLinks.getUrl(), equalTo("http://testuri"));
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/relativeLink", "http://absoluteLink/hello"));
        });
    }

    @Test
    public void shouldBeFineWithNullLinks() {
        CrawlResult crawlResult = CrawlResult.forSnapshot(
                aTestPageSnapshotForUri("http://testuri").withNullLinks().build());

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getCrawlId(), equalTo(TEST_CRAWL_ID));
            assertThat(outboundLinks.getUrl(), equalTo("http://testuri"));
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), hasSize(0));
        });
    }

    @Test
    public void shouldNotPersistFragments() {
        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("#this-is-a-fragment", "http://absoluteLink/hello#fragment");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://absoluteLink/hello"));
        });
    }

    @Test
    public void shouldDedupPagesWithFragments() {
        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("http://host/hello", "http://host/hello#fragment");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://host/hello"));
        });
    }

    @Test
    public void canHandleEmptyFragments() {
        CrawlResult crawlResult = givenCrawlResultForPageWithLinks("#", "http://host/page#");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http://host/page"));
        });
    }

    @Test
    public void shouldAlwaysPersistRelativeIfIsSameDomain() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
                "/link1",
                "http://domain/link2",
                "http://domain/some/path/link3",
                "http://anotherdomain/link1");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "/link2", "/some/path/link3", "http://anotherdomain/link1"));
        });
    }

    @Test
    public void shouldBeAbleToRelativizeUrlsWithSpaces() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
                "http://domain/link1 with spaces/salve");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1%20with%20spaces/salve"));
        });
    }

    @Test
    public void shouldRelativizeRelativeToCurrentUrls() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path/",
                "salve");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/some/path/salve"));
        });
    }

    @Test
    public void differentSchemaIsConsideredDifferentHost() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
                "/link1",
                "https://domain/link2");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "https://domain/link2"));
        });
    }

    @Test
    public void linksWithNoSlashAtTheBeginningShouldBeResolvedProperly() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/subpath/page", "link1");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/subpath/link1"));
        });
    }

    @Test
    public void invalidUrlShouldBePersistedAsTheyAre() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain/some/path",
                "http:-/link1__(this is invalid)__",
                "https://domain/link2");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("http:-/link1__(this is invalid)__", "https://domain/link2"));
        });
    }

    @Test
    public void shouldPersistDomain() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://something.domain/some/path", "/link1");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getDomain(), is("something.domain"));
        });
    }

    @Test
    public void shouldNotPersistJavascriptLinks() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
                "/link1",
                "javascript:void(0)");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1"));
        });
    }

    @Test
    public void linksShouldBeSorted() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
                "/c", "/d", "/c", "/b", "http://domain/a", "http://anotherdomain/b", "http://anotherdomain/a");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), Matchers.contains("/a", "/b", "/c", "/d", "http://anotherdomain/a", "http://anotherdomain/b"));
        });
    }

    @Test
    public void shouldMaintainTrailingSlashIfPresent() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
                "/link1/", "/link1");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertThat(outboundLinks.getLinksByType().get(LinkType.AHREF), containsInAnyOrder("/link1", "/link1/"));
        });
    }

    @Test
    public void shouldSaveCrawlDate() {
        CrawlResult crawlResult = givenCrawlResultForUrlWithPageWithLinks("http://domain",
                "/link1/", "/link1");

        sut.accept(crawlResult);

        verifySavedLinks(outboundLinks -> {
            assertNotNull(outboundLinks.getCrawledAt());
        });
    }

    private void verifySavedLinks(Consumer<OutboundLinks> linksVerify) {
        Mockito.verify(repository).save(ArgumentMatchers.argThat(argument -> {
            linksVerify.accept(argument);
            return true;
        }));
    }

    private CrawlResult givenCrawlResultForPageWithLinks(String... links) {
        return givenCrawlResultForUrlWithPageWithLinks("http://testuri", links);
    }

    private CrawlResult givenCrawlResultForUrlWithPageWithLinks(String url, String... links) {
        return CrawlResult.forSnapshot(aTestPageSnapshotForUri(url).withLinks(links).build());
    }
}
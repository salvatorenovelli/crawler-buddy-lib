package com.myseotoolbox.crawler;

import com.myseotoolbox.crawler.model.PageCrawl;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.model.RedirectChainElement;
import com.myseotoolbox.crawler.model.ResolvableField;
import com.myseotoolbox.crawler.testutils.CrawlHistoryTest;
import com.myseotoolbox.crawler.testutils.CrawlHistoryTestBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import java.util.List;

import static com.myseotoolbox.crawler.StandardMetaTagValues.*;
import static com.myseotoolbox.crawler.testutils.CrawlHistoryTestBuilder.*;
import static com.myseotoolbox.crawler.testutils.PageCrawlMatchers.referenceTo;
import static com.myseotoolbox.crawler.testutils.PageCrawlMatchers.valueType;
import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri;
import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.buildRedirectChainElementsFor;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PageCrawlBuilderTest implements CrawlHistoryTest {

    private PageCrawl prevCrawl;
    private PageSnapshot prevVal;
    private PageSnapshot curVal;

    private PageCrawlBuilder sut = new PageCrawlBuilder();


    @Override
    public void setValues(PageCrawl prevCrawl, PageSnapshot prevVal, PageSnapshot curVal) {
        this.prevCrawl = prevCrawl;
        this.prevVal = prevVal;
        this.curVal = curVal;
    }


    @Test
    public void shouldSetReferenceToPreCrawlIdIfItWasValueType() {


        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues()
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        String prevCrawlId = prevCrawl.getId();

        assertThat(pageCrawl.getRedirectChainElements().getReference(), is(prevCrawlId));
        assertThat(pageCrawl.getTitle().getReference(), is(prevCrawlId));
        assertThat(pageCrawl.getMetaDescriptions().getReference(), is(prevCrawlId));
        assertThat(pageCrawl.getH1s().getReference(), is(prevCrawlId));
        assertThat(pageCrawl.getH2s().getReference(), is(prevCrawlId));
        assertThat(pageCrawl.getCanonicals().getReference(), is(prevCrawlId));

    }

    @Test
    public void shouldSetReferencesOfPreviousFieldWhenSnapshotsAreEqual() {


        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues()
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getRedirectChainElements(), is(referenceTo(prevCrawl.getRedirectChainElements())));
        assertThat(pageCrawl.getTitle(), is(referenceTo(prevCrawl.getTitle())));
        assertThat(pageCrawl.getMetaDescriptions(), is(referenceTo(prevCrawl.getMetaDescriptions())));
        assertThat(pageCrawl.getH1s(), is(referenceTo(prevCrawl.getH1s())));
        assertThat(pageCrawl.getH2s(), is(referenceTo(prevCrawl.getH2s())));
        assertThat(pageCrawl.getCanonicals(), is(referenceTo(prevCrawl.getCanonicals())));

    }

    @Test
    public void shouldDetectDifferencesInRedirectChainLen() {

        List<RedirectChainElement> newRedirectChain = buildRedirectChainElementsFor(STANDARD_URI, 301, 200);

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withRedirectChainElements(newRedirectChain)
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getRedirectChainElements(), is(valueType(newRedirectChain)));

    }

    @Test
    public void shouldDetectDifferencesInRedirectChainStatus() {

        List<RedirectChainElement> newRedirectChain = buildRedirectChainElementsFor(STANDARD_URI, 201);

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withRedirectChainElements(newRedirectChain)
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getRedirectChainElements(), is(valueType(newRedirectChain)));

    }

    @Test
    public void shouldDetectDifferencesInRedirectChainDst() {

        List<RedirectChainElement> newRedirectChain = buildRedirectChainElementsFor(STANDARD_URI, 200);
        newRedirectChain.get(0).setDestinationURI("Another uri");

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withRedirectChainElements(newRedirectChain)
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getRedirectChainElements().getValue().get(0).getDestinationURI(), is("Another uri"));

    }

    @Test
    public void shouldDetectDifferencesInTitle() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withTitle("Different title")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getTitle(), is(valueType("Different title")));
    }

    @Test
    public void shouldDetectDifferencesInMetaDescription() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withMetaDescriptions("Some random value")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getMetaDescriptions(), is(valueType(singletonList("Some random value"))));
    }

    @Test
    public void shouldDetectDifferencesInH1s() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withH1s("First h1", "Second H1")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getH1s(), is(valueType(asList("First h1", "Second H1"))));
    }

    @Test
    public void shouldDetectDifferencesInH1sLenSmaller() {

        assertThat(standardPageSnapshot().getH1s(), hasSize(2));

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withH1s("First h1")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getH1s().getValue(), hasSize(1));
        assertThat(pageCrawl.getH1s(), is(valueType(singletonList("First h1"))));
    }

    @Test
    public void shouldDetectDifferencesInH1sLenBigger() {

        assertThat(standardPageSnapshot().getH1s(), hasSize(2));

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withH1s("First h1", "Second h1", "Third h1")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getH1s().getValue(), hasSize(3));
        assertThat(pageCrawl.getH1s(), is(valueType(asList("First h1", "Second h1", "Third h1"))));
    }

    @Test
    public void shouldDetectDifferencesInH2s() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withH2s("Some random value")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getH2s(), is(valueType(singletonList("Some random value"))));
    }

    @Test
    public void shouldDetectDifferencesInCanonicals() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withCanonicals("A different value")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);
        assertThat(pageCrawl.getCanonicals(), is(valueType(singletonList("A different value"))));
    }

    @Test
    public void shouldLeaveReferenceInFieldsThatDoNotChange() {


        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withCanonicals("A different value")
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getRedirectChainElements(), is(referenceTo(prevCrawl.getRedirectChainElements())));
        assertThat(pageCrawl.getTitle(), is(referenceTo(prevCrawl.getTitle())));
        assertThat(pageCrawl.getMetaDescriptions(), is(referenceTo(prevCrawl.getMetaDescriptions())));
        assertThat(pageCrawl.getH1s(), is(referenceTo(prevCrawl.getH1s())));
        assertThat(pageCrawl.getH2s(), is(referenceTo(prevCrawl.getH2s())));
        assertThat(pageCrawl.getCanonicals(), is(valueType(singletonList("A different value"))));

    }

    @Test
    public void ifPreviousValueCrawlWasNewValueItShouldTakeReferenceOfPreviousCrawlId() {

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().withTitle("Some different title").and()
                .withCurrentValue().havingStandardValueValues().withTitle("Some different title")
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getTitle(), referenceToCrawl(prevCrawl.getId()));
    }

    @Test
    public void ifTheValueChangeItShouldTakeValueEvenIfPreviousCrawlWasValueType() {

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCrawl().havingStandardValueValues().withTitle("Some different title1").and()
                .withCurrentValue().havingStandardValueValues().withTitle("Final title")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getTitle(), valueType("Final title"));
    }

    @Test
    public void canHandleNoPrevPageCrawl() {

        PageCrawl pageCrawl = sut.build(null, standardPageSnapshot(), null);

        assertThat(pageCrawl.getRedirectChainElements(), valueType(STANDARD_REDIRECT_CHAIN_ELEMENTS));
        assertThat(pageCrawl.getTitle(), valueType(STANDARD_TITLE));
        assertThat(pageCrawl.getMetaDescriptions(), valueType(STANDARD_META_DESCR));
        assertThat(pageCrawl.getH1s(), valueType(STANDARD_H1));
        assertThat(pageCrawl.getH2s(), valueType(STANDARD_H2));
        assertThat(pageCrawl.getCanonicals(), valueType(singletonList(pageCrawl.getUri())));

    }

    @Test
    public void nullRedirectChainElements() {

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().withRedirectChainElement(null).and()
                .withCurrentValue().havingStandardValueValues()
                .build();


        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getRedirectChainElements(), valueType(curVal.getRedirectChainElements()));
    }

    @Test
    public void canManageTemporary404() {

        givenCrawlHistory()
                .withCrawl().havingValue(standardPageSnapshot()).and()
                .withCrawl().havingValue(standardPageSnapshot()).and()
                .withCrawl().havingValue(a404PageSnapshot()).and()
                .withCurrentValue(standardPageSnapshot())
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getRedirectChainElements(), valueType(STANDARD_REDIRECT_CHAIN_ELEMENTS));
        assertThat(pageCrawl.getTitle(), valueType(STANDARD_TITLE));
        assertThat(pageCrawl.getMetaDescriptions(), valueType(STANDARD_META_DESCR));
        assertThat(pageCrawl.getH1s(), valueType(STANDARD_H1));
        assertThat(pageCrawl.getH2s(), valueType(STANDARD_H2));
        assertThat(pageCrawl.getCanonicals(), valueType(singletonList(pageCrawl.getUri())));
    }

    @Test
    public void testComplexChangeHistory() {

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCrawl().havingStandardValueValues().withTitle("Day 1").and()
                .withCrawl().havingStandardValueValues().withTitle("Day 1").withH1s("Day 2").and()
                .withCurrentValue(standardPageSnapshot()).withTitle("Day 1").withH1s("Day 2")
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getMetaDescriptions().getReference(), is(generateTestCrawlId(STANDARD_DATE, 0)));
        assertThat(pageCrawl.getTitle().getReference(), is(generateTestCrawlId(STANDARD_DATE, 1)));
        assertThat(pageCrawl.getH1s().getReference(), is(generateTestCrawlId(STANDARD_DATE, 2)));
    }


    @Test
    public void crawlDateIsPersisted() {
        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues()
                .build();

        PageCrawl pageCrawl = sut.build(prevVal, curVal, prevCrawl);

        assertThat(pageCrawl.getCreateDate(), is(curVal.getCreateDate()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfUriAreDifferent() {
        PageSnapshot prevVal = aPageSnapshotWithStandardValuesForUri("http://uri1");
        PageSnapshot curVal = aPageSnapshotWithStandardValuesForUri("http://uri2");

        sut.build(prevVal, curVal, prevCrawl);
    }

    @Test
    public void nullValuesArePersistedAsNullResolvableValueFields() {
        PageSnapshot snapshot = aPageSnapshotWithStandardValuesForUri("http://uri");
        snapshot.setTitle(null);
        PageCrawl build = sut.build(null, snapshot, null);

        assertTrue(build.getTitle().isValueField());
        assertNull(build.getTitle().getValue());
        assertNull(build.getTitle().getReference());
    }


    @Test
    public void shouldBeAbletoManageNullCurValue() {

        givenCrawlHistory()
                .withCrawl().havingStandardValueValues().and()
                .withCurrentValue().havingStandardValueValues().withTitle(null)
                .build();

        //Common when there is an error in crawl
        PageCrawl build = sut.build(prevVal, curVal, prevCrawl);

        assertNull(build.getTitle().getValue());

    }

    private CrawlHistoryTestBuilder givenCrawlHistoryForUri(String uri) {
        return new CrawlHistoryTestBuilder(this, uri);
    }

    private CrawlHistoryTestBuilder givenCrawlHistory() {
        return new CrawlHistoryTestBuilder(this);
    }

    private BaseMatcher<ResolvableField<?>> referenceToCrawl(String actual) {
        return new BaseMatcher<ResolvableField<?>>() {
            @Override
            public boolean matches(Object item) {

                ResolvableField<?> expected = (ResolvableField<?>) item;

                return expected.getReference().equals(actual);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(actual);
            }
        };
    }

}
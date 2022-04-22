package com.myseotoolbox.crawler.testutils;

import com.myseotoolbox.crawler.model.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.myseotoolbox.crawler.StandardMetaTagValues.STANDARD_DATE;
import static com.myseotoolbox.crawler.StandardMetaTagValues.STANDARD_URI;
import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aPageSnapshotWith404ValuesForUri;
import static com.myseotoolbox.crawler.testutils.PageSnapshotTestBuilder.aPageSnapshotWithStandardValuesForUri;
import static java.util.Arrays.asList;

public class CrawlHistoryTestBuilder {

    public static final String WEBSITE_CRAWL_ID = UUID.randomUUID().toString();
    private final Date crawlDate = STANDARD_DATE;
    private final CrawlHistoryTest testContext;
    private final String uri;

    private PageSnapshot prevValue;
    private PageSnapshot currentValue;
    private PageCrawl prevCrawl;
    private int crawlCounter = 0;

    public CrawlHistoryTestBuilder(CrawlHistoryTest testContext) {
        this.testContext = testContext;
        this.uri = STANDARD_URI;
    }

    public CrawlHistoryTestBuilder(CrawlHistoryTest testContext, String uri) {
        this.testContext = testContext;
        this.uri = uri;
    }

    public PrevValueBuilder withCrawl() {
        return new PrevValueBuilder();
    }

    private PageCrawl buildCrawlForAllValue(PageSnapshot value) {
        return new PageCrawl(generateCrawlId(), uri, crawlDate,
                ResolvableField.forValue(value.getRedirectChainElements()),
                ResolvableField.forValue(value.getTitle()),
                ResolvableField.forValue(value.getMetaDescriptions()),
                ResolvableField.forValue(value.getH1s()),
                ResolvableField.forValue(value.getH2s()),
                ResolvableField.forValue(value.getCanonicals()), value.getCrawlStatus());
    }

    private String generateCrawlId() {
        return generateTestCrawlId(crawlDate, crawlCounter++);
    }

    public static String generateTestCrawlId(Date crawlDate, int crawlCounter) {
        return "CRAWL_ID:" + crawlDate + "-" + crawlCounter;
    }


    public CrawlHistoryTestBuilder withCurrentValue(PageSnapshot value) {
        this.currentValue = value;
        return this;
    }


    public CrawlHistoryTestBuilder withCurrentValue() {
        return this;
    }

    public void build() {
        testContext.setValues(this.prevCrawl, this.prevValue, this.currentValue);
    }

    public CrawlHistoryTestBuilder havingStandardValueValues() {
        this.currentValue = aPageSnapshotWithStandardValuesForUri(uri);
        return this;
    }

    public CrawlHistoryTestBuilder withRedirectChainElements(List<RedirectChainElement> newRedirectChain) {
        this.currentValue.setRedirectChainElements(newRedirectChain);
        return this;
    }

    public CrawlHistoryTestBuilder withUri(String uri) {
        this.currentValue.setUri(uri);
        return this;
    }

    public CrawlHistoryTestBuilder withTitle(String title) {
        this.currentValue.setTitle(title);
        return this;
    }


    public CrawlHistoryTestBuilder withMetaDescriptions(String... metaDescriptions) {
        this.currentValue.setMetaDescriptions(asList(metaDescriptions));
        return this;
    }

    public CrawlHistoryTestBuilder withH1s(String... h1s) {
        this.currentValue.setH1s(asList(h1s));
        return this;
    }

    public CrawlHistoryTestBuilder withH2s(String... h2s) {
        this.currentValue.setH2s(asList(h2s));
        return this;
    }

    public CrawlHistoryTestBuilder withCanonicals(String... canonicals) {
        this.currentValue.setCanonicals(asList(canonicals));
        return this;
    }

    public class PrevValueBuilder {
        private PageSnapshot curValue;


        public PrevValueBuilder withRedirectChainElement(List<RedirectChainElement> elements) {
            this.curValue.setRedirectChainElements(elements);
            return this;
        }

        public PrevValueBuilder withTitle(String title) {
            this.curValue.setTitle(title);
            return this;
        }

        public PrevValueBuilder withH1s(String... h1s) {
            this.curValue.setH1s(Arrays.asList(h1s));
            return this;
        }

        public CrawlHistoryTestBuilder and() {


            if (CrawlHistoryTestBuilder.this.prevValue == null) {
                CrawlHistoryTestBuilder.this.prevCrawl = buildCrawlForAllValue(curValue);
            } else {
                CrawlHistoryTestBuilder.this.prevCrawl = buildPrevCrawl(CrawlHistoryTestBuilder.this.prevValue, curValue, CrawlHistoryTestBuilder.this.prevCrawl);
            }


            CrawlHistoryTestBuilder.this.prevValue = curValue;

            return CrawlHistoryTestBuilder.this;
        }

        public PageCrawl buildPrevCrawl(PageSnapshot prevValue,
                                        PageSnapshot curValue,
                                        PageCrawl prevCrawl) {


            PageCrawl base = new PageCrawl(curValue.getUri(), curValue.getCreateDate());
            base.setId(generateCrawlId());

            //In tests we don't mind the slowness of reflection yay!

            Stream.of(prevValue.getClass().getDeclaredMethods())
                    .filter(method -> method.getName().startsWith("get"))
                    .filter(method -> !asList("getId", "getCrawlStatus", "getUri", "getCreateDate", "getLinks").contains(method.getName()))
//                        .peek(method -> System.out.println(method.getName()))
                    .map(Method::getName)
                    .forEach(methodName -> {

                        try {

                            Method setterMethod = PageCrawl.class.getMethod(methodName.replaceAll("get", "set"), ResolvableField.class);

                            Method pageSnapshotMethod = PageSnapshot.class.getMethod(methodName);
                            Method pageCrawlMethod = PageCrawl.class.getMethod(methodName);

                            Object curField = pageSnapshotMethod.invoke(curValue);
                            Object prevField = pageSnapshotMethod.invoke(prevValue);

                            ResolvableField out;

                            if (curField.equals(prevField)) {

                                ResolvableField prevCrawlField = (ResolvableField) pageCrawlMethod.invoke(prevCrawl);

                                if (prevCrawlField.isValueField()) {
                                    out = ResolvableField.forReference(prevCrawl.getId());
                                } else {
                                    out = ResolvableField.forReference(prevCrawlField.getReference());
                                }
                            } else {
                                out = ResolvableField.forValue(curField);
                            }

                            setterMethod.invoke(base, out);


                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }


                    });


            return base;
        }

        public PrevValueBuilder havingStandardValueValues() {
            this.curValue = aPageSnapshotWithStandardValuesForUri(uri);
            return this;
        }

        public PrevValueBuilder havingValue(PageSnapshot value) {
            this.curValue = value;
            return this;
        }
    }

    public static PageSnapshot a404PageSnapshot() {
        return aPageSnapshotWith404ValuesForUri(STANDARD_URI);
    }

    public static PageSnapshot standardPageSnapshot() {
        return aPageSnapshotWithStandardValuesForUri(STANDARD_URI);
    }
}

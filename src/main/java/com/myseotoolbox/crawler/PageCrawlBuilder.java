package com.myseotoolbox.crawler;

import com.myseotoolbox.crawler.model.PageCrawl;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.model.ResolvableField;
import net.jodah.failsafe.internal.util.Assert;

import java.util.Objects;
import java.util.function.Function;

public class PageCrawlBuilder {


    public PageCrawl build(PageSnapshot prevValue, PageSnapshot curValue, PageCrawl prevCrawl) {

        sanitizeParams(prevValue, curValue, prevCrawl);

        PageCrawl pageCrawl = new PageCrawl(curValue.getUri(), curValue.getCreateDate());

        pageCrawl.setRedirectChainElements(compare(prevValue, curValue, prevCrawl, PageCrawl::getRedirectChainElements, PageSnapshot::getRedirectChainElements));
        pageCrawl.setTitle(compare(prevValue, curValue, prevCrawl, PageCrawl::getTitle, PageSnapshot::getTitle));
        pageCrawl.setMetaDescriptions(compare(prevValue, curValue, prevCrawl, PageCrawl::getMetaDescriptions, PageSnapshot::getMetaDescriptions));
        pageCrawl.setH1s(compare(prevValue, curValue, prevCrawl, PageCrawl::getH1s, PageSnapshot::getH1s));
        pageCrawl.setH2s(compare(prevValue, curValue, prevCrawl, PageCrawl::getH2s, PageSnapshot::getH2s));
        pageCrawl.setCanonicals(compare(prevValue, curValue, prevCrawl, PageCrawl::getCanonicals, PageSnapshot::getCanonicals));

        return pageCrawl;
    }

    private <T> ResolvableField<T> compare(PageSnapshot prevValue,
                                           PageSnapshot curValue,
                                           PageCrawl prevCrawl,
                                           Function<PageCrawl, ResolvableField<T>> pageCrawlFieldMapper,
                                           Function<PageSnapshot, T> snapshotMapper) {


        if (prevValue != null && Objects.equals(snapshotMapper.apply(curValue), snapshotMapper.apply(prevValue))) {

            ResolvableField<T> prevCrawlField = pageCrawlFieldMapper.apply(prevCrawl);

            if (prevCrawlField.isValueField()) {
                return ResolvableField.forReference(prevCrawl.getId());
            } else {
                return ResolvableField.forReference(prevCrawlField.getReference());
            }

        }

        return ResolvableField.forValue(snapshotMapper.apply(curValue));

    }

    private void sanitizeParams( PageSnapshot prevValue, PageSnapshot curValue,  PageCrawl prevCrawl) {
        if (prevValue == null || prevCrawl == null) {
            //If one is null, both have to be null
            Assert.isTrue(prevValue == null && prevCrawl == null, "PrevCrawl and PrevValue should be two view of the same state");
        } else {
            Assert.isTrue(prevValue.getUri().equals(curValue.getUri()) && prevValue.getUri().equals(prevCrawl.getUri()), "Uri Should be all the same");
        }
    }
}

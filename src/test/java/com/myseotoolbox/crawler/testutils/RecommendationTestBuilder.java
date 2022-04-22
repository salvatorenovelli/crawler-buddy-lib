//package com.myseotoolbox.crawler.testutils;
//
//
//import java.util.Arrays;
//
//public class RecommendationTestBuilder {
//
//    private final Recommendation cur;
//
//    public RecommendationTestBuilder() {
//        this.cur = new Recommendation();
//    }
//
//    public static RecommendationTestBuilder aRecommendation() {
//        return new RecommendationTestBuilder();
//    }
//
//    public RecommendationTestBuilder withTitle(String title) {
//        cur.setTitle(title);
//        return this;
//    }
//
//    public RecommendationTestBuilder withH1s(String... h1s) {
//        cur.setH1s(Arrays.asList(h1s));
//        return this;
//    }
//
//    public RecommendationTestBuilder withMetas(String... metas) {
//        cur.setMetaDescriptions(Arrays.asList(metas));
//        return this;
//    }
//
//    public Recommendation build() {
//        return cur;
//    }
//
//
//}

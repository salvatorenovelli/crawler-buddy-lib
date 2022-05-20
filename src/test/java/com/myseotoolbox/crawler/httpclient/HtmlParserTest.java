package com.myseotoolbox.crawler.httpclient;

import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.pagelinks.PageLink;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.myseotoolbox.crawler.httpclient.HtmlPageBuilder.givenHtmlPage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class HtmlParserTest {
    HtmlParser sut = new HtmlParser();

    @Test
    public void canFindTitleInsideHead() throws IOException {
        InputStream is = givenHtmlPage().withHeadElement("<TITLE>This is the head title</TITLE>").build();
        PageSnapshot parse = sut.parse("http://somehost", Collections.emptyList(), is);
        assertThat(parse.getTitle(), is("This is the head title"));
    }

    /**
     * Widely used browsers support this. Not clear if SEO is impacted
     * */
    @Test
    public void canFindTitleOutsideHead() throws IOException {
        InputStream is = givenHtmlPage().withBodyElement("<TITLE>This is the title</TITLE>").build();
        PageSnapshot parse = sut.parse("http://somehost", Collections.emptyList(), is);
        assertThat(parse.getTitle(), is("This is the title"));
    }

    @Test
    public void shouldPrioritizeTitleInHead() throws IOException {
        InputStream is = givenHtmlPage()
                .withHeadElement("<TITLE>This is the head title</TITLE>")
                .withBodyElement("<TITLE>This is the body title</TITLE>").build();

        PageSnapshot parse = sut.parse("http://somehost", Collections.emptyList(), is);
        assertThat(parse.getTitle(), is("This is the head title"));
    }

    @Test
    public void shouldSanitizeTags() throws IOException {
        InputStream is = givenHtmlPage()
                .withBodyElement("<h1><p>this contains a paragraph</p></h1>").and()
                .withBodyElement("<h2>this contains a <b>bold</b></h2>").build();

        PageSnapshot parse = sut.parse("http://somehost", Collections.emptyList(), is);
        assertThat(parse.getH1s().get(0), is("this contains a paragraph"));
        assertThat(parse.getH2s().get(0), is("this contains a bold"));
    }

    @Test
    public void linksShouldBeParsedWithAttributes() throws IOException {
        InputStream is = givenHtmlPage()
                .withBodyElement("<a href='/link' rel=\"nofollow\"></a>").build();

        PageSnapshot snapshot = sut.parse("http://somehost", Collections.emptyList(), is);
        List<PageLink> links = snapshot.getLinks();

        assertThat(links, hasSize(1));
        assertThat(links.get(0).getDestination(), is("/link"));
        assertThat(links.get(0).getAttributes().entrySet(), hasSize(1));
        Map.Entry<String, String> attribute = links.get(0).getAttributes().entrySet().iterator().next();
        assertThat(attribute.getKey(), is("rel"));
        assertThat(attribute.getValue(), is("nofollow"));
    }

    @Test
    public void linksShouldOnlyHaveRelAttribute() throws IOException {
        InputStream is = givenHtmlPage()
                .withBodyElement("<a href='/link' class=\"prettyLink\"></a>").build();

        PageSnapshot snapshot = sut.parse("http://somehost", Collections.emptyList(), is);
        List<PageLink> links = snapshot.getLinks();

        assertThat(links, hasSize(1));
        assertThat(links.get(0).getDestination(), is("/link"));
        assertThat(links.get(0).getAttributes().entrySet(), hasSize(0));
    }

    @Test
    public void linksHrefShouldNotBeRepresentedAsAttribute() throws IOException {
        InputStream is = givenHtmlPage()
                .withBodyElement("<a href='/link'></a>").build();

        PageSnapshot snapshot = sut.parse("http://somehost", Collections.emptyList(), is);
        List<PageLink> links = snapshot.getLinks();

        assertThat(links, hasSize(1));
        assertThat(links.get(0).getAttributes().entrySet(), hasSize(0));
    }
}
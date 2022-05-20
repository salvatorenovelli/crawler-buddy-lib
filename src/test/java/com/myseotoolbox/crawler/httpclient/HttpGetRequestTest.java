package com.myseotoolbox.crawler.httpclient;

import com.myseotoolbox.crawler.testutils.testwebsite.TestWebsiteBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class HttpGetRequestTest {

    public static final String LOCATION_WITH_UNICODE_CHARACTERS = "/família";

    TestWebsiteBuilder testWebsiteBuilder = TestWebsiteBuilder.build();
    private HttpURLConnectionFactory connectionFactory = new HttpURLConnectionFactory();

    @Before
    public void setUp() throws Exception {
        testWebsiteBuilder.run();
    }

    @After
    public void tearDown() throws Exception {
        testWebsiteBuilder.tearDown();
    }


    @Test
    public void itShouldBeAbleToDealWithDifferentCharsets() throws Exception {

        String basePath = "/тэг/уход-за-одеждой";

        givenAWebsite()
                .havingPage(basePath)
                .redirectingTo(301, "/teg/Ñ\u0083Ñ\u0085Ð¾Ð´").save();


        HttpResponse execute = new HttpGetRequest(testUri(basePath), connectionFactory).execute();
        assertThat(execute.getHttpStatus(), is(HTTP_MOVED_PERM));
        assertThat(execute.getLocation(), is(testUri("/teg/%C3%91%C2%83%C3%91%C2%85%C3%90%C2%BE%C3%90%C2%B4")));
    }

    @Test
    public void usesGetOn301WithRelativeDstPath() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(301, "/relative_destination").save();

        HttpResponse execute = new HttpGetRequest(testUri("/source"), connectionFactory).execute();

        assertThat(execute.getHttpStatus(), is(HTTP_MOVED_PERM));
        assertThat(execute.getLocation(), is(testUri("/relative_destination")));
    }

    @Test
    public void usesGetOn301WithAbsoluteDstPath() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(301, "http://absolute_destination").save();


        HttpResponse execute = new HttpGetRequest(testUri("/source"), connectionFactory).execute();

        assertThat(execute.getHttpStatus(), is(HTTP_MOVED_PERM));
        assertThat(execute.getLocation(), is(URI.create("http://absolute_destination")));
    }

    @Test
    public void testGetOn302Redirect() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(302, "/destination").save();

        HttpResponse execute = new HttpGetRequest(testUri("/source"), connectionFactory).execute();

        assertThat(execute.getHttpStatus(), is(HttpURLConnection.HTTP_MOVED_TEMP));
        assertThat(execute.getLocation(), is(testUri("/destination")));
    }

    @Test
    public void testGetOn303SeeOther() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(303, "/destination").save();

        HttpResponse execute = new HttpGetRequest(testUri("/source"), connectionFactory).execute();

        assertThat(execute.getHttpStatus(), is(HttpURLConnection.HTTP_SEE_OTHER));
        assertThat(execute.getLocation(), is(testUri("/destination")));
    }

    @Test
    public void getOn200() throws Exception {
        givenAWebsite()
                .havingPage("/hello").save();

        HttpResponse execute = new HttpGetRequest(testUri("/hello"), connectionFactory).execute();

        assertThat(execute.getHttpStatus(), is(HttpURLConnection.HTTP_OK));
        assertThat(execute.getLocation(), is(testUri("/hello")));
    }

    /**
     * Some website, put unicode characters in their target location (without escaping them).
     * <p>
     * The response bytes containing the unicode characters will end up being "decoded" into string with the default charset, and returned as a
     * header parameter. The problem is that the web server might have encoded them in a different charset than ours,
     * therefore we had to read the charset from the header and transcode the string into the intended charset.
     * <p>
     * <p>
     * PS: I used two redirects (firstPass, secondPass) as it made the test easier as we didn't have to assert against a unicode target URL.
     */
    @Test
    public void weShouldHandleUnicodeCharacters() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(301, LOCATION_WITH_UNICODE_CHARACTERS).and()
                .havingPage(LOCATION_WITH_UNICODE_CHARACTERS).redirectingTo(301, "/destination").save();


        HttpResponse firstPass = new HttpGetRequest(testUri("/source"), connectionFactory).execute();
        HttpResponse secondPass = new HttpGetRequest(firstPass.getLocation(), connectionFactory).execute();

        assertThat(secondPass.getHttpStatus(), is(HTTP_MOVED_PERM));
        assertThat(secondPass.getLocation(), is(testUri("/destination")));
    }

    @Test
    public void unicodeRedirectGetUrlEncoded() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(301, LOCATION_WITH_UNICODE_CHARACTERS).save();

        HttpResponse request = new HttpGetRequest(testUri("/source"), connectionFactory).execute();
        assertThat(request.getLocation(), is(testUri("/fam%C3%ADlia")));

    }

    @Test
    public void unicodeCharactersInLocationHeaderGetDecodedEvenWhenCharsetIsNotSpecified() throws Exception {
        givenAWebsite()
                .havingPage("/source").redirectingTo(301, LOCATION_WITH_UNICODE_CHARACTERS)
                .disableCharsetHeader()
                .save();

        HttpResponse request = new HttpGetRequest(testUri("/source"), connectionFactory).execute();
        assertThat(request.getLocation(), is(testUri("/fam%C3%ADlia")));

    }

    @Test
    public void shouldSetContentTypeInResponse() throws IOException, URISyntaxException {
        givenAWebsite()
                .havingPage("/hello").save();

        HttpResponse response = new HttpGetRequest(testUri("/hello"), connectionFactory).execute();

        assertThat(response.getContentType(), is("text/html"));
    }

    private URI testUri(String url) {
        return testWebsiteBuilder.buildTestUri(url);
    }

    private TestWebsiteBuilder givenAWebsite() {
        return testWebsiteBuilder;
    }
}

package com.myseotoolbox.crawler.httpclient;


import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import static com.myseotoolbox.crawler.httpclient.SafeStringEscaper.containsUnicodeCharacters;
import static com.myseotoolbox.crawler.spider.PageLinksHelper.toValidUri;
import static com.myseotoolbox.crawler.utils.IsRedirect.isRedirect;


@Log4j2
public class HttpGetRequest {

    public static final String BOT_NAME = "SpiderBuddy" ;
    public static final String BOT_VERSION = "1.0" ;
    public static final String USER_AGENT = "Mozilla/5.0 (compatible; " + BOT_NAME + "/" + BOT_VERSION + ")" ;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
    private final URI uri;
    private final NoSSLVerificationConnectionFactory connectionFactory;

    public HttpGetRequest(URI uri, NoSSLVerificationConnectionFactory connectionFactory) {
        this.uri = uri;
        this.connectionFactory = connectionFactory;
    }


    public HttpResponse execute() throws IOException, URISyntaxException {

        HttpURLConnection connection = connectionFactory.createConnection(new URI(uri.toASCIIString()));

        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        connection.connect();
        URI dstURI = uri;

        int status = connection.getResponseCode();

        if (isRedirect(status)) {
            dstURI = extractDestinationUri(connection, dstURI);
        }

        return new HttpResponse(status, dstURI, connection.getContentType(), status < 400 ? connection.getInputStream() : null);
    }

    private URI extractDestinationUri(HttpURLConnection connection, URI initialLocation) throws URISyntaxException {
        String locationHeader = connection.getHeaderField("location");


        if (containsUnicodeCharacters(locationHeader)) {
            log.warn("Redirect destination {} contains non ASCII characters (as required by the standard)", connection.getURL());
            locationHeader = SafeStringEscaper.escapeString(locationHeader);
        }

        URI location = toValidUri(locationHeader)
                .orElseThrow(() -> new IllegalArgumentException("Invalid redirect destination. src: " + initialLocation + " dst: " + connection.getHeaderField("location")));

        if (location.isAbsolute()) {
            return location;
        } else {
            return initialLocation.resolve(location);
        }
    }


}
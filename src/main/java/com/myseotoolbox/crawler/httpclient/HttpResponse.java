package com.myseotoolbox.crawler.httpclient;

import lombok.Data;

import java.io.InputStream;
import java.net.URI;


@Data
public class HttpResponse {
    private final int httpStatus;
    private final URI location;
    private final String contentType;
    private final InputStream inputStream;

    public HttpResponse(int httpStatus, URI location, String contentType, InputStream inputStream) {
        this.httpStatus = httpStatus;
        this.location = location;
        this.contentType = contentType;
        this.inputStream = inputStream;
    }
}

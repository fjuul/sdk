package com.fjuul.sdk.http;

import okhttp3.mockwebserver.MockWebServer;

public class TestHttpClientBuilder extends HttpClientBuilder {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public TestHttpClientBuilder(MockWebServer mockWebServer) {
        super(mockWebServer.url("/").toString(), TEST_API_KEY);
    }
}

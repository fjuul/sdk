package com.fjuul.sdk.http;

import okhttp3.mockwebserver.MockWebServer;

public class TestFjuulSDKApiHttpClientBuilder extends FjuulSDKApiHttpClientBuilder {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public TestFjuulSDKApiHttpClientBuilder(MockWebServer mockWebServer) {
        super(mockWebServer.url("/").toString(), TEST_API_KEY);
    }
}

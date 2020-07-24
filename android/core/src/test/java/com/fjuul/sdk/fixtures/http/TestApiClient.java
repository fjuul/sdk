package com.fjuul.sdk.fixtures.http;

import com.fjuul.sdk.http.ApiClient;

import okhttp3.mockwebserver.MockWebServer;

public final class TestApiClient {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public static class Builder extends ApiClient.Builder {
        public Builder(MockWebServer mockWebServer) {
            super(mockWebServer.url("/").toString(), TEST_API_KEY);
        }
    }
}

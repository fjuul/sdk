package com.fjuul.sdk.core.fixtures.http;

import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Keystore;

import okhttp3.mockwebserver.MockWebServer;

public final class TestApiClient {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public static class Builder extends ApiClient.Builder {
        // TODO: add constructor overloading for the stubbed application context
        public Builder(MockWebServer mockWebServer) {
            super(null, mockWebServer.url("/").toString(), TEST_API_KEY);
        }

        public Builder setKeystore(Keystore keystore) {
            this.keystore = keystore;
            return this;
        }

        @Override
        protected void setupDefaultKeystore() {
            // NOTE: do not setup default signing keystore for the test builder
        }
    }
}

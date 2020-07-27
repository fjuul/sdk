package com.fjuul.sdk.fixtures.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.ApiClient;

import okhttp3.mockwebserver.MockWebServer;

public final class TestApiClient {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public static class Builder extends ApiClient.Builder {
        // TODO: add constructor overloading for the stubbed application context
        public Builder(MockWebServer mockWebServer) {
            super(null, mockWebServer.url("/").toString(), TEST_API_KEY);
        }

        public Builder setSigningKeychain(SigningKeychain keychain) {
            this.signingKeychain = keychain;
            return this;
        }

        @Override
        protected void setupDefaultSigningKeychain() {
            // NOTE: do not setup default signing keychain for the test builder
        }
    }
}

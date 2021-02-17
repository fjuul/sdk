package com.fjuul.sdk.test.http;

import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Keystore;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import okhttp3.mockwebserver.MockWebServer;

public final class TestApiClient {
    public static final String TEST_API_KEY = "TEST_API_KEY";

    public static class Builder extends ApiClient.Builder {
        public Builder(@NonNull MockWebServer mockWebServer) {
            this(ApplicationProvider.getApplicationContext(), mockWebServer);
        }

        public Builder(@NonNull Context context, @NonNull MockWebServer mockWebServer) {
            super(context, mockWebServer.url("/").toString(), TEST_API_KEY);
        }

        @NonNull
        public Builder setKeystore(@NonNull Keystore keystore) {
            this.keystore = keystore;
            return this;
        }

        @Override
        protected void setupDefaultStorage() {
            // NOTE: do not setup default signing keystore for the test builder
        }
    }
}

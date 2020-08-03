package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.Keystore;
import com.fjuul.sdk.entities.PersistentStorage;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.http.interceptors.BearerAuthInterceptor;
import com.fjuul.sdk.http.interceptors.SigningAuthInterceptor;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;

/**
 * Main entry point to communicate with Fjuul API from a user identity.
 *
 * <p>
 * Use ApiClient.Builder to create instance of this class.
 *
 * <p>
 * NOTE: reuse the created instance as much as possible to share the same signing mechanism between services and prevent
 * refreshing collision.
 *
 * @see ApiClient.Builder
 */
public class ApiClient {
    private String baseUrl;
    private String apiKey;
    private Keystore userKeystore;
    private UserCredentials userCredentials;
    private SigningAuthInterceptor signingAuthInterceptor;

    private ApiClient(String baseUrl, String apiKey, Keystore keychain, UserCredentials credentials) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.userKeystore = keychain;
        this.userCredentials = credentials;
    }

    /**
     * Besides constructor parameters, user credentials and singing keychain must be initialized through according
     * setters.
     */
    public static class Builder {
        private String baseUrl;
        private String apiKey;
        protected @NonNull Context appContext;
        protected @Nullable Keystore keystore;
        protected @Nullable UserCredentials userCredentials;

        /**
         * @param baseUrl the API base URL to connect to, e.g. `https://api.fjuul.com`.
         * @param apiKey the API key.
         */
        // TODO: add the overloaded constructor with an environment parameter
        public Builder(@NonNull Context appContext, @NonNull String baseUrl, @NonNull String apiKey) {
            this.appContext = appContext;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
        }

        /**
         * The method must be invoked if it's planned to build an api-client with an ability of signing requests (it's
         * very frequent case). An user credentials is needed to issue or refresh signing keys.
         *
         * @param userCredentials valid user credentials to authenticate an identity.
         */
        public @NonNull Builder setUserCredentials(@NonNull UserCredentials userCredentials) {
            this.userCredentials = userCredentials;
            return this;
        }

        protected void setupDefaultKeystore() {
            if (appContext != null && userCredentials != null) {
                this.keystore = new Keystore(new PersistentStorage(appContext), userCredentials.getToken());
            }
        }

        public @NonNull ApiClient build() {
            setupDefaultKeystore();
            return new ApiClient(baseUrl, apiKey, keystore, userCredentials);
        }
    }

    public @NonNull String getBaseUrl() {
        return baseUrl;
    }

    public @NonNull String getUserToken() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials");
        }
        return userCredentials.getToken();
    }

    public @NonNull OkHttpClient buildSigningClient(@NonNull ISigningService signingService) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(getOrCreateSigningAuthInterceptor(signingService))
            .authenticator(getOrCreateSigningAuthInterceptor(signingService))
            .build();
        return client;
    }

    public @NonNull OkHttpClient buildSigningClient() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials to build a signing client");
        }
        return buildSigningClient(new UserSigningService(this));
    }

    public @NonNull OkHttpClient buildUserAuthorizedClient() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials to build an authenticated client");
        }
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(new BearerAuthInterceptor(userCredentials))
            .build();
        return client;
    }

    // TODO: consider moving this code to the builder class (share the same instance of the interceptor/authenticator)
    protected @NonNull SigningAuthInterceptor getOrCreateSigningAuthInterceptor(
        @NonNull ISigningService signingService) {
        if (signingAuthInterceptor == null) {
            signingAuthInterceptor = new SigningAuthInterceptor(userKeystore, new RequestSigner(), signingService);
        }
        return signingAuthInterceptor;
    }
}

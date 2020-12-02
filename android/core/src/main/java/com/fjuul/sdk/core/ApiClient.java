package com.fjuul.sdk.core;

import com.fjuul.sdk.core.entities.IStorage;
import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.PersistentStorage;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.core.http.interceptors.BearerAuthInterceptor;
import com.fjuul.sdk.core.http.interceptors.SigningAuthInterceptor;
import com.fjuul.sdk.core.http.services.ISigningService;
import com.fjuul.sdk.core.http.services.UserSigningService;
import com.fjuul.sdk.core.http.utils.RequestSigner;

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
    private @NonNull String baseUrl;
    private @NonNull String apiKey;
    private @NonNull Context appContext;
    private @NonNull IStorage storage;
    private @NonNull Keystore userKeystore;
    private @Nullable UserCredentials userCredentials;
    private @Nullable SigningAuthInterceptor signingAuthInterceptor;

    private ApiClient(String baseUrl, String apiKey, Context appContext, IStorage storage, Keystore userKeystore, UserCredentials credentials) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.appContext = appContext;
        this.storage = storage;
        this.userKeystore = userKeystore;
        this.userCredentials = credentials;
    }

    /**
     * Besides constructor parameters, user credentials and others may be initialized through the appropriate setters.
     */
    public static class Builder {
        private @NonNull String baseUrl;
        private @NonNull String apiKey;
        protected @NonNull Context appContext;
        protected @Nullable IStorage storage;
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

        protected void setupDefaultStorage() {
            if (appContext == null) {
                throw new IllegalArgumentException("Application context must not be null");
            }
            storage = new PersistentStorage(appContext);
            if (userCredentials != null) {
                this.keystore = new Keystore(new PersistentStorage(appContext), userCredentials.getToken());
            }
        }

        public @NonNull ApiClient build() {
            setupDefaultStorage();
            return new ApiClient(baseUrl, apiKey, appContext, storage, keystore, userCredentials);
        }
    }

    public @NonNull String getBaseUrl() {
        return baseUrl;
    }

    public @NonNull String getApiKey() {
        return apiKey;
    }

    public @NonNull String getUserToken() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials");
        }
        return userCredentials.getToken();
    }

    public @NonNull String getUserSecret() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials");
        }
        return userCredentials.getSecret();
    }

    public @NonNull IStorage getStorage() {
        return storage;
    }

    public @NonNull Context getAppContext() {
        return appContext;
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

    public @NonNull OkHttpClient buildClient() {
        OkHttpClient client =
            new OkHttpClient().newBuilder().addInterceptor(new ApiKeyAttachingInterceptor(apiKey)).build();
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

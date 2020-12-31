package com.fjuul.sdk.core;

import com.fjuul.sdk.core.entities.IStorage;
import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.PersistentStorage;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.core.http.interceptors.BearerAuthInterceptor;
import com.fjuul.sdk.core.http.interceptors.SDKUserAgentInterceptor;
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
 * <p>
 * Use ApiClient.Builder to create instance of this class.
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

    private ApiClient(String baseUrl,
        String apiKey,
        Context appContext,
        IStorage storage,
        Keystore userKeystore,
        UserCredentials credentials) {
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
            this.appContext = appContext.getApplicationContext();
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

    /**
     * Deletes the stored user file of the shared preferences created internally for persisting the state of Fjuul SDK.
     * Note that if you want to perform the logout, then you need also to disable all background works in
     * {@code ActivitySourcesManager} in the {@code activitysources} module. Otherwise, the file will be re-created
     * again on the next background work.
     * @param context application context
     * @param userToken token of a user to delete
     * @return boolean which indicates the success of the operation
     */
    public static boolean clearPersistentStorage(@NonNull Context context, @NonNull String userToken) {
        return new PersistentStorage(context, userToken).remove();
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

    /**
     * Deletes the stored user file of the shared preferences created internally for persisting the state of Fjuul SDK.
     * Note that if you want to perform the logout, then you need also to disable all background works in
     * {@code ActivitySourcesManager} in the {@code activitysources} module. Otherwise, the file will be re-created
     * again on the next background work.<br>
     * This method throws IllegalStateException if no set user credentials.<br>
     * After calling this you must be sure that nothing doesn't refer to this instance of {@code ApiClient} because
     * after the storage is removed, any write/read operations will throw the exception.
     * @throws IllegalStateException when no user credentials
     * @return boolean which indicates the success of the operation
     */
    public boolean clearPersistentStorage() {
        return clearPersistentStorage(appContext, getUserToken());
    }

    public @NonNull IStorage getStorage() {
        return storage;
    }

    public @NonNull Context getAppContext() {
        return appContext;
    }

    public @NonNull OkHttpClient buildSigningClient(@NonNull ISigningService signingService) {
        OkHttpClient client =
            createCommonClientBuilder().addInterceptor(getOrCreateSigningAuthInterceptor(signingService))
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
        OkHttpClient client =
            createCommonClientBuilder().addInterceptor(new BearerAuthInterceptor(userCredentials)).build();
        return client;
    }

    public @NonNull OkHttpClient buildClient() {
        return createCommonClientBuilder().build();
    }

    // TODO: consider moving this code to the builder class (share the same instance of the interceptor/authenticator)
    protected @NonNull SigningAuthInterceptor getOrCreateSigningAuthInterceptor(
        @NonNull ISigningService signingService) {
        if (signingAuthInterceptor == null) {
            signingAuthInterceptor = new SigningAuthInterceptor(userKeystore, new RequestSigner(), signingService);
        }
        return signingAuthInterceptor;
    }

    private OkHttpClient.Builder createCommonClientBuilder() {
        return new OkHttpClient.Builder().addInterceptor(new SDKUserAgentInterceptor(BuildConfig.VERSION_NAME))
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey));
    }
}

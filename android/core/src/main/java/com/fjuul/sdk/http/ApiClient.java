package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.http.interceptors.BearerAuthInterceptor;
import com.fjuul.sdk.http.interceptors.SigningAuthInterceptor;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

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
    private SigningKeychain userKeychain;
    private UserCredentials userCredentials;
    private SigningAuthInterceptor signingAuthInterceptor;

    private ApiClient(String baseUrl, String apiKey, SigningKeychain keychain, UserCredentials credentials) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.userKeychain = keychain;
        this.userCredentials = credentials;
    }

    /**
     * Besides constructor parameters, user credentials and singing keychain must be initialized through according
     * setters.
     */
    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private SigningKeychain signingKeychain;
        private UserCredentials userCredentials;

        /**
         * @param baseUrl the API base URL to connect to, e.g. `https://api.fjuul.com`.
         * @param apiKey the API key.
         */
        // TODO: add the overloaded constructor with an environment parameter
        public Builder(String baseUrl, String apiKey) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
        }

        public Builder setUserCredentials(UserCredentials userCredentials) {
            this.userCredentials = userCredentials;
            return this;
        }

        public Builder setSigningKeychain(SigningKeychain keychain) {
            this.signingKeychain = keychain;
            return this;
        }

        public ApiClient build() {
            return new ApiClient(baseUrl, apiKey, signingKeychain, userCredentials);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUserToken() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials");
        }
        return userCredentials.getToken();
    }

    public OkHttpClient buildSigningClient(ISigningService signingService) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(getOrCreateSigningAuthInterceptor(signingService))
            .authenticator(getOrCreateSigningAuthInterceptor(signingService))
            .build();
        return client;
    }

    public OkHttpClient buildSigningClient() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needed user credentials to build a signing client");
        }
        return buildSigningClient(new UserSigningService(this));
    }

    public OkHttpClient buildUserAuthorizedClient() {
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
    protected SigningAuthInterceptor getOrCreateSigningAuthInterceptor(ISigningService signingService) {
        if (signingAuthInterceptor == null) {
            signingAuthInterceptor = new SigningAuthInterceptor(userKeychain, new RequestSigner(), signingService);
        }
        return signingAuthInterceptor;
    }
}

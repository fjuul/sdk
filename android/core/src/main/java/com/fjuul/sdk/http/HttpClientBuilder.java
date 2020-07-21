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

public class HttpClientBuilder {
    private String baseUrl;
    private String apiKey;
    private SigningKeychain userKeychain;
    private UserCredentials userCredentials;
    private SigningAuthInterceptor signingAuthInterceptor;

    // TODO: add the overloaded constructor with an environment parameter
    public HttpClientBuilder(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public void setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
    }

    public void setUserKeychain(SigningKeychain keychain) {
        this.userKeychain = keychain;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public SigningKeychain getUserSigningKeychain() {
        if (userKeychain == null) {
            throw new IllegalStateException("The builder requires the user keychain");
        }
        return userKeychain;
    }

    // TODO: consider returning the retrofit client
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
            throw new IllegalStateException("The builder needs user credentials to build a signing client");
        }
        return buildSigningClient(new UserSigningService(this));
    }

    public OkHttpClient buildUserAuthorizedClient() {
        if (userCredentials == null) {
            throw new IllegalStateException("The builder needs user credentials to build an authenticated client");
        }
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(new BearerAuthInterceptor(userCredentials))
            .build();
        return client;
    }

    protected SigningAuthInterceptor getOrCreateSigningAuthInterceptor(ISigningService signingService) {
        if (signingAuthInterceptor == null) {
            signingAuthInterceptor = new SigningAuthInterceptor(userKeychain, new RequestSigner(), signingService);
        }
        return signingAuthInterceptor;
    }
}

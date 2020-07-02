package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.http.interceptors.SigningInterceptor;
import com.fjuul.sdk.http.interceptors.UserAuthInterceptor;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.OkHttpClient;

public class FjuulSDKApiHttpClientBuilder {
    private String baseUrl;
    private String apiKey;

    // TODO: add base-url
    // TODO: add the overloaded constructor with an environment parameter
    public FjuulSDKApiHttpClientBuilder(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // TODO: consider returning the retrofit client
    public OkHttpClient buildSigningClient(SigningKeychain keychain) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(new SigningInterceptor(keychain, new RequestSigner()))
            .build();
        return client;
    }

    public OkHttpClient buildUserAuthorizedClient(UserCredentials credentials) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
            .addInterceptor(new UserAuthInterceptor(credentials))
            .build();
        return client;
    }
}

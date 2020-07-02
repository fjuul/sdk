package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.http.interceptors.SigningInterceptor;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.OkHttpClient;

public class FjuulSDKApiHttpClientBuilder {
    String serviceKey;

    public FjuulSDKApiHttpClientBuilder(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    // TODO: consider returning the retrofit client
    public OkHttpClient buildSigningClient(SigningKeychain keychain) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(serviceKey))
            .addInterceptor(new SigningInterceptor(keychain, new RequestSigner()))
            .build();
        return client;
    }

    public OkHttpClient buildAuthorizedClient(String token, String secret) {
        // TODO: apply interceptor for authentication
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new ApiKeyAttachingInterceptor(serviceKey))
            .build();
        return client;
    }
}

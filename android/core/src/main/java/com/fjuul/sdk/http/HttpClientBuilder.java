package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.interceptors.ApiKeyAttachingInterceptor;
import com.fjuul.sdk.http.interceptors.SigningInterceptor;
import com.fjuul.sdk.http.interceptors.UserAuthInterceptor;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.OkHttpClient;

public class HttpClientBuilder {
    private String baseUrl;
    private String apiKey;

    // TODO: add the overloaded constructor with an environment parameter
    public HttpClientBuilder(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // TODO: consider returning the retrofit client
    public OkHttpClient buildSigningClient(
            SigningKeychain keychain, ISigningService signingService) {
        OkHttpClient client =
                new OkHttpClient()
                        .newBuilder()
                        .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
                        .addInterceptor(
                                new SigningInterceptor(
                                        keychain, new RequestSigner(), signingService))
                        .build();
        return client;
    }

    public OkHttpClient buildSigningClient(SigningKeychain keychain, UserCredentials credentials) {
        return buildSigningClient(keychain, new UserSigningService(this, credentials));
    }

    public OkHttpClient buildUserAuthorizedClient(UserCredentials credentials) {
        OkHttpClient client =
                new OkHttpClient()
                        .newBuilder()
                        .addInterceptor(new ApiKeyAttachingInterceptor(apiKey))
                        .addInterceptor(new UserAuthInterceptor(credentials))
                        .build();
        return client;
    }
}

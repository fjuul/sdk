package com.fjuul.sdk.http;

import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.interceptors.SigningInterceptor;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.OkHttpClient;

public class FjuulSDKApiHttpClient {
    public static OkHttpClient buildSigningClient(SigningKeychain keychain) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new SigningInterceptor(keychain, new RequestSigner()))
            .build();
        return client;
    }
}

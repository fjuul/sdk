package com.fjuul.sdk.http.interceptors;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiKeyAttachingInterceptor implements Interceptor {
    String apiKey;

    public ApiKeyAttachingInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        return chain.proceed(requestBuilder.addHeader("x-api-key", apiKey).build());
    }
}

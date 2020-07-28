package com.fjuul.sdk.http.interceptors;

import java.io.IOException;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiKeyAttachingInterceptor implements Interceptor {
    String apiKey;

    public ApiKeyAttachingInterceptor(@NonNull String apiKey) {
        this.apiKey = apiKey;
    }

    @SuppressLint({"UnknownNullness"})
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        return chain.proceed(requestBuilder.addHeader("x-api-key", apiKey).build());
    }
}

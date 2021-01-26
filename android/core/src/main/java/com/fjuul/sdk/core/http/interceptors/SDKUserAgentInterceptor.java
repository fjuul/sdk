package com.fjuul.sdk.core.http.interceptors;

import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SDKUserAgentInterceptor implements Interceptor {
    @NonNull
    private final String agent;

    public SDKUserAgentInterceptor(@NonNull String versionName) {
        agent = String.format(Locale.ROOT, "Fjuul Android SDK %s", versionName);
    }

    @SuppressLint("UnknownNullness")
    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request.Builder builder = chain.request().newBuilder();
        final Request newRequest = builder.header("User-Agent", agent).build();
        return chain.proceed(newRequest);
    }
}

package com.fjuul.sdk.core.http.interceptors;

import java.io.IOException;

import com.fjuul.sdk.core.entities.UserCredentials;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BearerAuthInterceptor implements Interceptor {
    UserCredentials credentials;

    public BearerAuthInterceptor(@NonNull UserCredentials credentials) {
        this.credentials = credentials;
    }

    @SuppressLint("UnknownNullness")
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        Request newRequest = builder.header("Authorization", credentials.getCompleteAuthString()).build();
        return chain.proceed(newRequest);
    }
}

package com.fjuul.sdk.http.interceptors;

import java.io.IOException;

import com.fjuul.sdk.entities.UserCredentials;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAuthInterceptor implements Interceptor {
    UserCredentials credentials;

    public UserAuthInterceptor(UserCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        Request newRequest =
                builder.header("Authorization", credentials.getCompleteAuthString()).build();
        return chain.proceed(newRequest);
    }
}

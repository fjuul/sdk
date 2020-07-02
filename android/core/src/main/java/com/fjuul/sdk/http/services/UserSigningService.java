package com.fjuul.sdk.http.services;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.FjuulSDKApiHttpClientBuilder;
import com.fjuul.sdk.http.apis.SigningApi;

import java.io.IOException;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserSigningService {
    private SigningApi signingApiClient;

    public UserSigningService(FjuulSDKApiHttpClientBuilder clientBuilder, UserCredentials credentials) {
        OkHttpClient httpClient = clientBuilder.buildUserAuthorizedClient(credentials);
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(clientBuilder.getBaseUrl())
            .client(httpClient)
            .build();
        signingApiClient = retrofit.create(SigningApi.class);
    }

    public Response<SigningKey> issueUserKey() throws IOException {
        return signingApiClient.issueUserKey().execute();
    }
}

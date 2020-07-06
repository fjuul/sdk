package com.fjuul.sdk.http.services;

import java.io.IOException;
import java.util.Date;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.FjuulSDKApiHttpClientBuilder;
import com.fjuul.sdk.http.apis.SigningApi;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class UserSigningService implements ISigningService {
    private SigningApi signingApiClient;

    public UserSigningService(
            FjuulSDKApiHttpClientBuilder clientBuilder, UserCredentials credentials) {
        OkHttpClient httpClient = clientBuilder.buildUserAuthorizedClient(credentials);
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(clientBuilder.getBaseUrl())
                        .client(httpClient)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build();
        signingApiClient = retrofit.create(SigningApi.class);
    }

    @Override
    public Observable<Result<SigningKey>> issueKey() throws IOException {
        return signingApiClient.issueUserKey();
    }
}

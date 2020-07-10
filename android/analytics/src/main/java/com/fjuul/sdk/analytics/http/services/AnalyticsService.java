package com.fjuul.sdk.analytics.http.services;

import java.io.IOException;
import java.util.Date;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.apis.AnalyticsApi;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.FjuulSDKApiHttpClientBuilder;
import com.fjuul.sdk.http.services.ISigningService;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class AnalyticsService {
    private AnalyticsApi analyticsApiClient;

    public AnalyticsService(
            FjuulSDKApiHttpClientBuilder clientBuilder,
            SigningKeychain keychain,
            ISigningService signingService) {
        OkHttpClient httpClient = clientBuilder.buildSigningClient(keychain, signingService);
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(clientBuilder.getBaseUrl())
                        .client(httpClient)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build();
        analyticsApiClient = retrofit.create(AnalyticsApi.class);
    }

    public Call<DailyStats> getDailyStats(String userToken, String date)
            throws IOException {
        return analyticsApiClient.getDailyStats(userToken, date);
    }

    public Call<DailyStats[]> getDailyStats(
            String userToken, String startDate, String endDate) throws IOException {
        return analyticsApiClient.getDailyStats(userToken, startDate, endDate);
    }
}

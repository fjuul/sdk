package com.fjuul.sdk.analytics.http.services;

import java.io.IOException;
import java.util.Date;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.apis.AnalyticsApi;
import com.fjuul.sdk.http.HttpClientBuilder;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `AnalyticsService` encapsulates access to a users fitness and activity data.
 */
public class AnalyticsService {
    private AnalyticsApi analyticsApiClient;
    private HttpClientBuilder clientBuilder;

    /**
     * Create instance of the analytics api service.
     * @param clientBuilder configured client builder with signing ability and user credentials
     */
    public AnalyticsService(HttpClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        OkHttpClient httpClient = clientBuilder.buildSigningClient();
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(clientBuilder.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        analyticsApiClient = retrofit.create(AnalyticsApi.class);
    }

    /**
     * Builds the call to get the daily activity statistics for a given day.
     * @param date the day in format 'YYYY-MM-DD' to request daily stats for; this is the date in the users local timezone.
     * @return ApiCall for the user activity statistics for the given day.
     * @throws IOException
     */
    public ApiCall<DailyStats> getDailyStats(String date) throws IOException {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), date);
    }

    /**
     * Builds the call to get the daily activity statistics for a given day interval.
     * @param startDate the start of the day interval in format 'YYYY-MM-DD' to request daily stats for (inclusive);
     *                  this is the date in the users local timezone.
     * @param endDate the end of the day interval in format 'YYYY-MM-DD' to request daily stats for (inclusive);
     *                this is the date in the users local timezone.
     * @return ApiCall for the user activity statistics for the given day interval.
     * @throws IOException
     */
    public ApiCall<DailyStats[]> getDailyStats(String startDate, String endDate) throws IOException {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), startDate, endDate);
    }
}

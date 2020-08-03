package com.fjuul.sdk.analytics.http.services;

import java.util.Date;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.apis.AnalyticsApi;
import com.fjuul.sdk.http.ApiClient;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `AnalyticsService` encapsulates access to a users fitness and activity data.
 */
public class AnalyticsService {
    private AnalyticsApi analyticsApiClient;
    private ApiClient clientBuilder;

    /**
     * Create instance of the analytics api service.
     *
     * @param client configured client with signing ability and user credentials
     */
    public AnalyticsService(@NonNull ApiClient client) {
        this.clientBuilder = client;
        OkHttpClient httpClient = client.buildSigningClient();
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(client.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        analyticsApiClient = retrofit.create(AnalyticsApi.class);
    }

    /**
     * Builds the call to get the daily activity statistics for a given day.
     *
     * @param date the day in format 'YYYY-MM-DD' to request daily stats for; this is the date in the users local
     *        timezone.
     * @return ApiCall for the user activity statistics for the given day.
     */
    public @NonNull ApiCall<DailyStats> getDailyStats(@NonNull String date) {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), date);
    }

    /**
     * Builds the call to get the daily activity statistics for a given day interval.
     *
     * @param startDate the start of the day interval in format 'YYYY-MM-DD' to request daily stats for (inclusive);
     *        this is the date in the users local timezone.
     * @param endDate the end of the day interval in format 'YYYY-MM-DD' to request daily stats for (inclusive); this is
     *        the date in the users local timezone.
     * @return ApiCall for the user activity statistics for the given day interval.
     */
    public @NonNull ApiCall<DailyStats[]> getDailyStats(@NonNull String startDate, @NonNull String endDate) {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), startDate, endDate);
    }
}

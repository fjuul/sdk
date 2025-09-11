package com.fjuul.sdk.analytics.http.services;

import java.time.LocalDate;
import java.util.Date;

import com.fjuul.sdk.analytics.entities.AggregatedDailyStats;
import com.fjuul.sdk.analytics.entities.AggregationType;
import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.apis.AnalyticsApi;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `AnalyticsService` encapsulates access to a user's fitness and activity data.
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
     * @param date the day to request daily stats for.
     * @return ApiCall for the user activity statistics for the given day.
     */
    public @NonNull ApiCall<DailyStats> getDailyStats(@NonNull LocalDate date) {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), date.toString());
    }

    /**
     * Builds the call to get the daily activity statistics for a given day interval.
     *
     * @param startDate the start of the day interval to request daily stats for (inclusive); this is the date in the
     *        users local timezone.
     * @param endDate the end of the day interval to request daily stats for (inclusive); this is the date in the users
     *        local timezone.
     * @return ApiCall for the user activity statistics for the given day interval.
     */
    public @NonNull ApiCall<DailyStats[]> getDailyStats(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        return analyticsApiClient.getDailyStats(clientBuilder.getUserToken(), startDate.toString(), endDate.toString());
    }

    /**
     * Builds a call to get sums or averages of the daily activity statistics within a given date range.
     *
     * @param startDate the start of the interval to request daily stats aggregate for (inclusive); this is the date in
     *        the users local timezone.
     * @param endDate the end of the interval to request daily stats aggregate for (inclusive); this is the date in the
     *        users local timezone.
     * @param aggregation aggregation type; sum or average.
     * @return ApiCall for the user activity statistics for the given day interval.
     */
    public @NonNull ApiCall<AggregatedDailyStats> getAggregatedDailyStats(@NonNull LocalDate startDate,
        @NonNull LocalDate endDate,
        @NonNull AggregationType aggregation) {
        return analyticsApiClient.getAggregatedDailyStats(clientBuilder.getUserToken(),
            startDate.toString(),
            endDate.toString(),
            aggregation.toString());
    }
}

package com.fjuul.sdk.analytics.http.apis;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.entities.AggregatedDailyStats;
import com.fjuul.sdk.core.http.utils.ApiCall;

import androidx.annotation.NonNull;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AnalyticsApi {
    @GET("/sdk/analytics/v1/daily-stats/{userToken}/{date}")
    @NonNull
    ApiCall<DailyStats> getDailyStats(@Path("userToken") @NonNull String userToken, @Path("date") @NonNull String date);

    @GET("/sdk/analytics/v1/daily-stats/{userToken}")
    @NonNull
    ApiCall<DailyStats[]> getDailyStats(@Path("userToken") @NonNull String userToken,
        @Query("from") @NonNull String startDate,
        @Query("to") @NonNull String endDate);

    @GET("/sdk/analytics/v1/daily-stats/{userToken}/aggregated")
    @NonNull
    ApiCall<AggregatedDailyStats> getAggregatedDailyStats(@Path("userToken") @NonNull String userToken,
        @Query("from") @NonNull String startDate,
        @Query("to") @NonNull String endDate,
        @Query("aggregation") @NonNull String aggregation);
}

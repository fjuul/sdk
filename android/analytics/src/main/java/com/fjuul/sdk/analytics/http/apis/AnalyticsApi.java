package com.fjuul.sdk.analytics.http;

import com.fjuul.sdk.analytics.entities.DailyStats;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AnalyticsApi {
    @GET("/analytics/v1/daily-stats/{userToken}/{date}")
    Call<DailyStats> getDailyStats(@Path("userToken") String userToken);

    @GET("/analytics/v1/daily-stats/{userToken}")
    Call<DailyStats> getDailyStats(@Path("userToken") String userToken, @Query("startDate") String startDate, @Query("endDate") String endDate);
}

package com.fjuul.sdk.analytics.http.apis;

import com.fjuul.sdk.analytics.entities.DailyStats;

import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AnalyticsApi {
    @GET("/sdk/analytics/v1/daily-stats/{userToken}/{date}")
    CompletableFuture<Response<DailyStats>> getDailyStats(
            @Path("userToken") String userToken, @Path("date") String date);

    @GET("/sdk/analytics/v1/daily-stats/{userToken}")
    CompletableFuture<Response<DailyStats[]>> getDailyStats(
            @Path("userToken") String userToken,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate);
}
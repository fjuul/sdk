package com.fjuul.sdk.activitysources.http.apis;

import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.http.utils.ApiCall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ActivitySourcesApi {
    @POST("/sdk/activity-sources/v1/{userToken}/connections/{activitySource}")
    @NonNull
    ApiCall<ResponseBody> connect(@Path("userToken") @NonNull String userToken,
        @Path("activitySource") @NonNull String activitySource);

    @DELETE("/sdk/activity-sources/v1/{userToken}/connections/{connectionId}")
    @NonNull
    ApiCall<Void> disconnect(@Path("userToken") @NonNull String userToken,
        @Path("connectionId") @NonNull String activitySource);

    @GET("/sdk/activity-sources/v1/{userToken}/connections")
    @NonNull
    ApiCall<TrackerConnection[]> getConnections(@Path("userToken") @NonNull String userToken,
        @Query("show") @Nullable String show);
}

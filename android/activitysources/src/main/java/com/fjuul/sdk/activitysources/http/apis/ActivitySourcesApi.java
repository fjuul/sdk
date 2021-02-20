package com.fjuul.sdk.activitysources.http.apis;

import java.util.Map;

import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.activitysources.entities.internal.GFSynchronizableProfileParams;
import com.fjuul.sdk.activitysources.entities.internal.GFUploadData;
import com.fjuul.sdk.core.http.utils.ApiCall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ActivitySourcesApi {
    @POST("/sdk/activity-sources/v1/{userToken}/connections/{activitySource}")
    @NonNull
    ApiCall<ResponseBody> connect(@Path("userToken") @NonNull String userToken,
        @Path("activitySource") @NonNull String activitySource,
        @QueryMap @NonNull Map<String, String> queryMap);

    @DELETE("/sdk/activity-sources/v1/{userToken}/connections/{connectionId}")
    @NonNull
    ApiCall<Void> disconnect(@Path("userToken") @NonNull String userToken,
        @Path("connectionId") @NonNull String activitySource);

    @GET("/sdk/activity-sources/v1/{userToken}/connections")
    @NonNull
    ApiCall<TrackerConnection[]> getConnections(@Path("userToken") @NonNull String userToken,
        @Query("show") @Nullable String show);

    @NonNull
    @POST("/sdk/activity-sources/v1/{userToken}/googlefit")
    ApiCall<Void> uploadGoogleFitData(@Path("userToken") @NonNull String userToken, @Body @NonNull GFUploadData data);

    @PUT("/sdk/activity-sources/v1/{userToken}/googlefit/profile")
    @NonNull
    ApiCall<Void> updateProfileOnBehalfOfGoogleFit(@Path("userToken") @NonNull String userToken,
        @Body @NonNull GFSynchronizableProfileParams params);
}

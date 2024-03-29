package com.fjuul.sdk.user.http.apis;

import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.user.entities.UserCreationResult;
import com.fjuul.sdk.user.entities.UserProfile;

import androidx.annotation.NonNull;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApi {
    @GET("/sdk/users/v1/{userToken}")
    @NonNull
    ApiCall<UserProfile> getProfile(@Path("userToken") @NonNull String userToken);

    @PUT("/sdk/users/v1/{userToken}")
    @NonNull
    ApiCall<UserProfile> updateProfile(@Path("userToken") @NonNull String userToken,
        @NonNull @Body UserProfile.PartialBuilder profileBuilder);

    @POST("/sdk/users/v1")
    @NonNull
    ApiCall<UserCreationResult> create(@NonNull @Body UserProfile.PartialBuilder profileBuilder);

    @DELETE("/sdk/users/v1/{userToken}")
    @NonNull
    ApiCall<Void> markForDeletion(@Path("userToken") @NonNull String userToken);
}

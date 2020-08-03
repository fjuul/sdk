package com.fjuul.sdk.http.apis;

import com.fjuul.sdk.entities.SigningKey;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.http.GET;

public interface SigningApi {
    @GET("/sdk/signing/v1/issue-key/user")
    @NonNull
    public Call<SigningKey> issueUserKey();
}

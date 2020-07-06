package com.fjuul.sdk.http.apis;

import com.fjuul.sdk.entities.SigningKey;

import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;

public interface SigningApi {
    @GET("/sdk/signing/v1/issue-key/user")
    public Observable<Result<SigningKey>> issueUserKey();
}

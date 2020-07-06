package com.fjuul.sdk.http.services;

import java.io.IOException;

import com.fjuul.sdk.entities.SigningKey;

import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;

public interface ISigningService {
    Observable<Result<SigningKey>> issueKey() throws IOException;
}

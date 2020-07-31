package com.fjuul.sdk.http.services;

import java.io.IOException;

import com.fjuul.sdk.entities.SigningKey;

import retrofit2.Call;

public interface ISigningService {
    Call<SigningKey> issueKey() throws IOException;
}

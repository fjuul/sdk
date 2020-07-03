package com.fjuul.sdk.http.services;

import com.fjuul.sdk.entities.SigningKey;

import java.io.IOException;

import retrofit2.Call;

public interface ISigningService {
    Call<SigningKey> issueKey() throws IOException;
}

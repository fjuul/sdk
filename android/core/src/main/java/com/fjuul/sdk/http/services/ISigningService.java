package com.fjuul.sdk.http.services;

import java.io.IOException;

import com.fjuul.sdk.entities.SigningKey;

import androidx.annotation.NonNull;
import retrofit2.Call;

public interface ISigningService {
    @NonNull
    Call<SigningKey> issueKey() throws IOException;
}

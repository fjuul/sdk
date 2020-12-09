package com.fjuul.sdk.core.http.services;

import java.io.IOException;

import com.fjuul.sdk.core.entities.SigningKey;

import androidx.annotation.NonNull;
import retrofit2.Call;

public interface ISigningService {
    @NonNull
    Call<SigningKey> issueKey() throws IOException;
}

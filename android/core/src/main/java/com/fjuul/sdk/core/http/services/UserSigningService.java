package com.fjuul.sdk.core.http.services;

import java.io.IOException;
import java.util.Date;

import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.SigningKey;
import com.fjuul.sdk.core.http.apis.SigningApi;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class UserSigningService implements ISigningService {
    private SigningApi signingApiClient;

    public UserSigningService(@NonNull ApiClient client) {
        OkHttpClient httpClient = client.buildUserAuthorizedClient();
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(client.getBaseUrl())
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        signingApiClient = retrofit.create(SigningApi.class);
    }

    @Override
    public @NonNull Call<SigningKey> issueKey() throws IOException {
        return signingApiClient.issueUserKey();
    }
}

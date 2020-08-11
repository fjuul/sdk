package com.fjuul.sdk.user.http.services;

import androidx.annotation.NonNull;

import com.fjuul.sdk.http.ApiClient;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.fjuul.sdk.user.entities.UserCreationResult;
import com.fjuul.sdk.user.entities.UserProfile;
import com.fjuul.sdk.user.http.apis.UserApi;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import java.util.Date;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * TODO: javadoc
 */
public class UserService {
    private UserApi userApiClient;
    private ApiClient clientBuilder;

    /**
     * Create instance of the user api service.
     *
     * @param client configured client with signing ability and user credentials
     */
    public UserService(@NonNull ApiClient client) {
        this.clientBuilder = client;
        OkHttpClient httpClient = client.buildSigningClient();
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(client.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        userApiClient = retrofit.create(UserApi.class);
    }

    @NonNull
    public ApiCall<UserCreationResult> createUser(@NonNull UserProfile.PartialBuilder builder) {
        UserProfile partialProfile = builder.buildPartialProfile();
        return userApiClient.create(partialProfile);
    }

    @NonNull
    public ApiCall<UserProfile> getProfile() {
        return userApiClient.getProfile(clientBuilder.getUserToken());
    }

    @NonNull
    public ApiCall<UserProfile> updateProfile(@NonNull UserProfile.PartialBuilder builder) {
        UserProfile partialProfile = builder.buildPartialProfile();
        return userApiClient.updateProfile(clientBuilder.getUserToken(),partialProfile);
    }
}

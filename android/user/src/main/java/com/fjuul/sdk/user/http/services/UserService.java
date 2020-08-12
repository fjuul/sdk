package com.fjuul.sdk.user.http.services;

import androidx.annotation.NonNull;

import com.fjuul.sdk.http.ApiClient;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.fjuul.sdk.user.adapters.LocalDateJsonAdapter;
import com.fjuul.sdk.user.adapters.TimeZoneJsonAdapter;
import com.fjuul.sdk.user.entities.UserCreationResult;
import com.fjuul.sdk.user.entities.UserProfile;
import com.fjuul.sdk.user.http.apis.UserApi;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import java.time.LocalDate;
import java.util.Date;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * TODO: javadoc
 */
public class UserService {
    private UserApi userApiClient;
    private UserApi signingUserApiClient;
    private ApiClient clientBuilder;

    /**
     * Create instance of the user api service.
     *
     * @param client configured client with signing ability and user credentials
     */
    public UserService(@NonNull ApiClient client) {
        this.clientBuilder = client;
    }

    private Retrofit createRetrofit(OkHttpClient httpClient) {
        Moshi moshi = new Moshi.Builder()
            .add(Date.class, new Rfc3339DateJsonAdapter())
            .add(new LocalDateJsonAdapter())
            .add(new TimeZoneJsonAdapter())
            .build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(clientBuilder.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        return retrofit;
    }

    private UserApi getOrCreateUserApi() {
        if (this.userApiClient != null) {
            return userApiClient;
        }
        synchronized (this) {
            Retrofit retrofit = createRetrofit(clientBuilder.buildClient());
            this.userApiClient = retrofit.create(UserApi.class);
            return this.userApiClient;
        }
    }

    private UserApi getOrCreateSigningUserApi() {
        if (this.signingUserApiClient != null) {
            return signingUserApiClient;
        }
        synchronized (this) {
            Retrofit retrofit = createRetrofit(clientBuilder.buildSigningClient());
            this.signingUserApiClient = retrofit.create(UserApi.class);
            return this.signingUserApiClient;
        }
    }

    @NonNull
    public ApiCall<UserCreationResult> createUser(@NonNull UserProfile.PartialBuilder builder) {
        return getOrCreateUserApi().create(builder);
    }

    @NonNull
    public ApiCall<UserProfile> getProfile() {
        return getOrCreateSigningUserApi().getProfile(clientBuilder.getUserToken());
    }

    @NonNull
    public ApiCall<UserProfile> updateProfile(@NonNull UserProfile.PartialBuilder builder) {
        return getOrCreateSigningUserApi().updateProfile(clientBuilder.getUserToken(), builder);
    }
}

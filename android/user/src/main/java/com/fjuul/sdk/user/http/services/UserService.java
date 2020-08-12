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

import java.util.Date;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `UserService` encapsulates access to a users profile data.
 */
public class UserService {
    private UserApi userApiClient;
    private UserApi signingUserApiClient;
    private ApiClient clientBuilder;

    /**
     * Create instance of the user api service.
     *
     * @param client configured client. No need to setup the user credentials for a user creation.
     *               When user credentials is obtained then recreate an instance of UserService with
     *               a new client configured with the user credentials.
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

    /**
     * Builds the call to create a new user with the given profile params.
     * @param builder profile builder with all required params.
     * @return ApiCall for the user creation.
     */
    @NonNull
    public ApiCall<UserCreationResult> createUser(@NonNull UserProfile.PartialBuilder builder) {
        return getOrCreateUserApi().create(builder);
    }

    /**
     * Builds the call to get a user profile by the user credentials specified at the client
     * configuration.
     * @return ApiCall for the user profile.
     */
    @NonNull
    public ApiCall<UserProfile> getProfile() {
        return getOrCreateSigningUserApi().getProfile(clientBuilder.getUserToken());
    }

    /**
     * Builds the call to update a user profile with the given params.
     * @param builder profile builder with parameters. This method supports a partial update so it
     *                will be enough to assign only parameters those need to update.
     * @return ApiCall for the update of user profile.
     */
    @NonNull
    public ApiCall<UserProfile> updateProfile(@NonNull UserProfile.PartialBuilder builder) {
        return getOrCreateSigningUserApi().updateProfile(clientBuilder.getUserToken(), builder);
    }
}

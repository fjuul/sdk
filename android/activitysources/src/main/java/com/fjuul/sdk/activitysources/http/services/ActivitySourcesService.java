package com.fjuul.sdk.activitysources.http.services;

import java.util.Date;
import java.util.TimeZone;

import com.fjuul.sdk.activitysources.entities.ConnectionResult;
import com.fjuul.sdk.activitysources.http.ActivitySourcesApiResponseTransformer;
import com.fjuul.sdk.activitysources.http.apis.ActivitySourcesApi;
import com.fjuul.sdk.http.ApiClient;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `ActivitySourcesService` encapsulates the management of a users activity sources.
 */
public class ActivitySourcesService {
    private ActivitySourcesApi apiClient;
    private ApiClient clientBuilder;

    /**
     * Create instance of the activity sources api service.
     *
     * @param client configured client with signing ability and user credentials
     */
    public ActivitySourcesService(@NonNull ApiClient client) {
        this.clientBuilder = client;
        OkHttpClient httpClient = client.buildSigningClient();
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        ActivitySourcesApiResponseTransformer responseTransformer = new ActivitySourcesApiResponseTransformer();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(client.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create(responseTransformer))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        apiClient = retrofit.create(ActivitySourcesApi.class);
    }

    /**
     * Builds the call to create a connection to the given activity source
     *
     * @return ApiCall for the connection creation.
     */
    @NonNull
    public ApiCall<ConnectionResult> connect(@NonNull String activitySource) {
        return (ApiCall)apiClient.connect(clientBuilder.getUserToken(), activitySource);
    }
}

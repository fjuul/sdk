package com.fjuul.sdk.activitysources.http.services;

import java.util.Date;

import com.fjuul.sdk.activitysources.entities.ConnectionResult;
import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.activitysources.http.ActivitySourcesApiResponseTransformer;
import com.fjuul.sdk.activitysources.http.apis.ActivitySourcesApi;
import com.fjuul.sdk.http.ApiClient;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * The `ActivitySourcesService` encapsulates the management of a users activity sources.
 */
public class ActivitySourcesService {
    private ActivitySourcesApi apiClient;
    private ApiClient clientBuilder;

    @NonNull
    public String getUserToken() {
        return clientBuilder.getUserToken();
    }

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
     * Builds the call to create a connection to the given activity source. Returning connection result
     * (ConnectionResult) has 2 cases:<br>
     * 1) requirement for external authentication: the implementor needs to take this URL and call it via an external
     * web browser (not a web view!). This initiates the oauth handshake. If successfully concluded the user is
     * redirected into the app to a URL that looks like this:
     * {@literal `fjuulsdk://external_connect?service=[tracker]&success=[true|false]`}. Depending on the outcome the
     * flag `success` is set to true or false. The "protocol" depends on the tenant the user belongs to.<br>
     * 2) no need for authentication: connection to the given tracker was created.
     *
     * In case of attempt to connect to the already connected tracker, the api call result will have error
     * ActivitySourcesApiErrors.SourceAlreadyConnectedError.
     *
     * @see ConnectionResult
     * @see com.fjuul.sdk.activitysources.errors.ActivitySourcesApiErrors.SourceAlreadyConnectedError
     * @return ApiCall for the connection creation.
     */
    @NonNull
    public ApiCall<ConnectionResult> connect(@NonNull String activitySource) {
        return (ApiCall) apiClient.connect(clientBuilder.getUserToken(), activitySource);
    }

    /**
     * Build the call to deactivate the given tracker connection.
     *
     * @param connection tracker connection to deactivate
     * @return ApiCall for the deactivation.
     */
    @NonNull
    public ApiCall<Void> disconnect(@NonNull TrackerConnection connection) {
        return apiClient.disconnect(clientBuilder.getUserToken(), connection.getId());
    }

    /**
     * Build the call to fetch all user's active connections
     *
     * @return ApiCall for the fetching connections
     */
    @NonNull
    public ApiCall<TrackerConnection[]> getCurrentConnections() {
        return apiClient.getConnections(clientBuilder.getUserToken(), "current");
    }
}

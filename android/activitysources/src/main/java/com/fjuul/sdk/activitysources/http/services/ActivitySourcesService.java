package com.fjuul.sdk.activitysources.http.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fjuul.sdk.activitysources.adapters.GFUploadDataJsonAdapter;
import com.fjuul.sdk.activitysources.adapters.HCUploadDataJsonAdapter;
import com.fjuul.sdk.activitysources.entities.ConnectionResult;
import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.activitysources.entities.internal.GFSynchronizableProfileParams;
import com.fjuul.sdk.activitysources.entities.internal.GFUploadData;
import com.fjuul.sdk.activitysources.entities.internal.HCSynchronizableProfileParams;
import com.fjuul.sdk.activitysources.entities.internal.HCUploadData;
import com.fjuul.sdk.activitysources.exceptions.ActivitySourcesApiExceptions;
import com.fjuul.sdk.activitysources.http.ActivitySourcesApiResponseTransformer;
import com.fjuul.sdk.activitysources.http.apis.ActivitySourcesApi;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallAdapterFactory;
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
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter())
            .add(new GFUploadDataJsonAdapter())
            .add(new HCUploadDataJsonAdapter())
            .build();
        ActivitySourcesApiResponseTransformer responseTransformer = new ActivitySourcesApiResponseTransformer();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(client.getBaseUrl())
            .client(httpClient)
            .addCallAdapterFactory(ApiCallAdapterFactory.create(responseTransformer))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build();
        apiClient = retrofit.create(ActivitySourcesApi.class);
    }

    /**
     * Builds the call to create a connection to the given activity source. The returning connection result
     * ({@link ConnectionResult}) has 2 possible cases:
     * <ol>
     * <li>requirement for external authentication: the implementor needs to take this URL and call it via an external
     * web browser (not a web view!). This initiates the oauth handshake. If successfully concluded the user is
     * redirected into the app to a URL that looks like this:
     * {@code fjuulsdk://external_connect?service=[tracker]&success=[true|false]}. Depending on the outcome the flag
     * `success` is set to true or false. The "protocol" depends on the tenant the user belongs to.</li>
     * <li>no need for authentication: connection to the given tracker was created.</li>
     * </ol>
     * In case of an attempt to connect to the already connected tracker, the api call result will have
     * {@link ActivitySourcesApiExceptions.SourceAlreadyConnectedException}.
     *
     * @see ConnectionResult
     * @see ActivitySourcesApiExceptions.SourceAlreadyConnectedException
     * @return ApiCall for the connection creation.
     */
    @NonNull
    public ApiCall<ConnectionResult> connect(@NonNull String activitySource) {
        return (ApiCall) apiClient.connect(clientBuilder.getUserToken(), activitySource, new HashMap<>());
    }

    @NonNull
    public ApiCall<ConnectionResult> connect(@NonNull String activitySource, @NonNull Map<String, String> queryParams) {
        return (ApiCall) apiClient.connect(clientBuilder.getUserToken(), activitySource, queryParams);
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
     * Build the call to fetch all user's active connections.
     *
     * @return ApiCall for the fetching connections
     */
    @NonNull
    public ApiCall<TrackerConnection[]> getCurrentConnections() {
        return apiClient.getConnections(clientBuilder.getUserToken(), "current");
    }

    /**
     * Build the call to send the GoogleFit fitness data for processing.
     *
     * @param dataToUpload GoogleFit data to upload
     * @return ApiCall for uploading the fitness data
     */
    @NonNull
    public ApiCall<Void> uploadGoogleFitData(@NonNull GFUploadData dataToUpload) {
        return apiClient.uploadGoogleFitData(clientBuilder.getUserToken(), dataToUpload);
    }

    /**
     * Build the call to update the user profile with marking that changes originated from Google Fit. The method is
     * intended to be used for syncing the user profile with Google Fit. the GoogleFit fitness data for processing.
     *
     * @param params - profile parameters
     * @return ApiCall for updating the user profile
     */
    @NonNull
    public ApiCall<Void> updateProfileOnBehalfOfGoogleFit(@NonNull GFSynchronizableProfileParams params) {
        return apiClient.updateProfileOnBehalfOfGoogleFit(clientBuilder.getUserToken(), params);
    }

    /**
     * Build the call to send the Google Health Connect fitness data for processing.
     *
     * @param dataToUpload Google Health Connect data to upload
     * @return ApiCall for uploading the fitness data
     */
    @NonNull
    public ApiCall<Void> uploadHealthConnectData(@NonNull HCUploadData dataToUpload) {
        return apiClient.uploadHealthConnectData(clientBuilder.getUserToken(), dataToUpload);
    }

    /**
     * Build the call to update the user profile with marking that changes originated from Google Health Connect. The
     * method is intended to be used for syncing the user profile with Google Health Connect.
     *
     * @param params - profile parameters
     * @return ApiCall for updating the user profile
     */
    @NonNull
    public ApiCall<Void> updateProfileOnBehalfOfHealthConnect(@NonNull HCSynchronizableProfileParams params) {
        return apiClient.updateProfileOnBehalfOfHealthConnect(clientBuilder.getUserToken(), params);
    }
}

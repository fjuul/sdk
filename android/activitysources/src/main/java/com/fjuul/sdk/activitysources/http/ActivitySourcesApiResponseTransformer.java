package com.fjuul.sdk.activitysources.http;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.regex.Pattern;

import com.fjuul.sdk.activitysources.entities.ConnectionResult;
import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.activitysources.exceptions.ActivitySourcesApiExceptions.SourceAlreadyConnectedException;
import com.fjuul.sdk.core.http.responses.ErrorJSONBodyResponse;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.core.http.utils.DefaultApiResponseTransformer;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import androidx.annotation.NonNull;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ActivitySourcesApiResponseTransformer extends DefaultApiResponseTransformer {
    private final JsonAdapter<ExternalAuthenticationFlowRequired> externalAuthenticationJsonAdapter;
    private final JsonAdapter<TrackerConnection> connectionJsonAdapter;
    private final Pattern createConnectionPathPattern = Pattern.compile("/connections/[^/]+$");

    public ActivitySourcesApiResponseTransformer() {
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        externalAuthenticationJsonAdapter = moshi.adapter(ExternalAuthenticationFlowRequired.class);
        connectionJsonAdapter = moshi.adapter(TrackerConnection.class);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    public ApiCallResult transform(@NonNull Response response) {
        final String requestPath = response.raw().request().url().encodedPath();
        final String requestMethod = response.raw().request().method();
        if (requestMethod.equals("POST") && createConnectionPathPattern.asPredicate().test(requestPath)) {
            ResponseBody responseBody = (ResponseBody) response.body();
            if (response.code() == HttpURLConnection.HTTP_OK) {
                try {
                    ExternalAuthenticationFlowRequired trackerAuthentication =
                        externalAuthenticationJsonAdapter.fromJson(responseBody.source());
                    return ApiCallResult.value(trackerAuthentication);
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            } else if (response.code() == HttpURLConnection.HTTP_CREATED) {
                try {
                    TrackerConnection trackerConnection = connectionJsonAdapter.fromJson(responseBody.source());
                    ConnectionResult connectionResult = new ConnectionResult.Connected(trackerConnection);
                    return ApiCallResult.value(connectionResult);
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            } else if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                ErrorJSONBodyResponse errorBody = extractErrorJsonBodyResponse(response);
                final String errorMessage =
                    errorBody != null && errorBody.getMessage() != null && !errorBody.getMessage().isEmpty()
                        ? errorBody.getMessage()
                        : "Tracker already connected";
                return ApiCallResult.error(new SourceAlreadyConnectedException(errorMessage));
            }
        }
        return super.transform(response);
    }
}

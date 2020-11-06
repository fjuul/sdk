package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.entities.Callback;
import com.fjuul.sdk.entities.Result;
import com.fjuul.sdk.errors.FjuulError;
import com.fjuul.sdk.http.ApiClient;

import java.util.List;

public final class ActivitySourcesManager {
    private ActivitySourcesService sourcesService;
    private ActivitySourcesStateStore stateStore;
    @Nullable private List<TrackerConnection> currentConnections;

    @Nullable private volatile static ActivitySourcesManager instance;

    ActivitySourcesManager(ActivitySourcesService sourcesService, ActivitySourcesStateStore stateStore, List<TrackerConnection> connections) {
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.currentConnections = connections;
    }

    @SuppressLint("NewApi")
    public static synchronized void initialize(ApiClient client) {
        // (get the configured api-client from context ?)
        ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage(), client.getUserToken());
        List<TrackerConnection> connections = stateStore.getConnections().orElse(null);
        ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        GoogleFitActivitySource.initialize(client);
        instance = new ActivitySourcesManager(sourcesService, stateStore, connections);
    }

    @NonNull
    public static ActivitySourcesManager getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("You must initialize first before getting the instance");
        }
        return instance;
    }

    // TODO: describe the google-fit behavior
    public void connect(@NonNull final ActivitySource activitySource, @NonNull final Callback<Intent> callback) {
        if (activitySource instanceof GoogleFitActivitySource) {
            final Intent intent = ((GoogleFitActivitySource) activitySource).buildIntentRequestingPermissions();
            callback.onResult(Result.value(intent));
            return;
        }
        final String trackerValue = activitySource.getTrackerValue().getValue();
        sourcesService.connect(trackerValue).enqueue((apiCall, apiCallResult) -> {
            if (apiCallResult.isError()) {
                callback.onResult(Result.error(apiCallResult.getError()));
                return;
            }
            ConnectionResult connectionResult = apiCallResult.getValue();
            if (connectionResult instanceof ExternalAuthenticationFlowRequired) {
                final String url = ((ExternalAuthenticationFlowRequired) connectionResult).getUrl();
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                callback.onResult(Result.value(intent));
            } else {
                callback.onResult(Result.error(new FjuulError("Activity source was already connected")));
            }
        });
    }
}

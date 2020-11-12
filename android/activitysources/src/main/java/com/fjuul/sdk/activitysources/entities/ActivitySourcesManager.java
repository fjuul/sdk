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
import com.fjuul.sdk.exceptions.FjuulException;
import com.fjuul.sdk.http.ApiClient;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ActivitySourcesManager {
    private final ActivitySourcesService sourcesService;
    private final ActivitySourcesStateStore stateStore;
    @Nullable private volatile List<TrackerConnection> currentConnections;

    @Nullable private volatile static ActivitySourcesManager instance;

    ActivitySourcesManager(@NonNull ActivitySourcesService sourcesService,
                           @NonNull ActivitySourcesStateStore stateStore,
                           @Nullable List<TrackerConnection> connections) {
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.currentConnections = connections;
    }

    @SuppressLint("NewApi")
    public static synchronized void initialize(ApiClient client) {
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
                callback.onResult(Result.error(new FjuulException("Activity source was already connected")));
            }
        });
    }

    public void disconnect(@NonNull final ActivitySourceConnection sourceConnection, @NonNull final Callback<List<ActivitySourceConnection>> callback) {
        // TODO: validate if sourceConnection was already ended ?
        Runnable runnableDisconnect = () -> {
            sourcesService.disconnect(sourceConnection).enqueue((call, apiCallResult) -> {
                if (apiCallResult.isError()) {
                    if (callback != null) {
                        callback.onResult(Result.error(apiCallResult.getError()));
                    }
                    return;
                }
                refreshCurrent(callback);
            });
        };
        final ActivitySource activitySource = sourceConnection.getActivitySource();
        if (activitySource instanceof GoogleFitActivitySource) {
            Task<Void> disableGoogleFitTask = ((GoogleFitActivitySource) activitySource).disable();
            disableGoogleFitTask.addOnCompleteListener((task) -> {
                // TODO: ensure if is it possible to have an error here on the second disable
                // TODO: run the disconnect if even there was an error?
                if (task.isCanceled() || !task.isSuccessful()) {
                    callback.onResult(Result.error(task.getException()));
                    return;
                }
                runnableDisconnect.run();
            });
        } else {
            runnableDisconnect.run();
        }
    }

    @SuppressLint("NewApi")
    @Nullable
    public List<ActivitySourceConnection> getCurrent() {
        final List<TrackerConnection> currentConnections = this.currentConnections;
        if (currentConnections == null) {
            return null;
        }
        return convertTrackerConnectionsToActivitySourcesConnections(currentConnections);
    }

    public void refreshCurrent(@Nullable Callback<List<ActivitySourceConnection>> callback) {
        sourcesService.getCurrentConnections().enqueue((call, apiCallResult) -> {
            if (apiCallResult.isError()) {
                if (callback != null) {
                    callback.onResult(Result.error(apiCallResult.getError()));
                }
                return;
            }
            final List<TrackerConnection> freshTrackerConnections = Arrays.asList(apiCallResult.getValue());
            final List<ActivitySourceConnection> sourceConnections = convertTrackerConnectionsToActivitySourcesConnections(freshTrackerConnections);
            stateStore.setConnections(freshTrackerConnections);
            this.currentConnections = freshTrackerConnections;
            if (callback != null) {
                callback.onResult(Result.value(sourceConnections));
            }
        });
    }

    @SuppressLint("NewApi")
    private static List<ActivitySourceConnection> convertTrackerConnectionsToActivitySourcesConnections(@NonNull List<TrackerConnection> trackerConnections) {
        Stream<ActivitySourceConnection> sourceConnectionsStream = trackerConnections.stream()
            .map(connection -> {
                ActivitySource activitySource = null;
                switch (ActivitySource.TrackerValue.forValue(connection.getTracker())) {
                    case POLAR:
                        activitySource = PolarActivitySource.getInstance();
                        break;
                    case FITBIT:
                        activitySource = FitbitActivitySource.getInstance();
                        break;
                    case GARMIN:
                        activitySource = GarminActivitySource.getInstance();
                        break;
                    case GOOGLE_FIT:
                        activitySource = GoogleFitActivitySource.getInstance();
                        break;
                    default: break;
                }
                if (activitySource == null) {
                    return null;
                }
                return new ActivitySourceConnection(connection, activitySource);
            }).filter(Objects::nonNull);
        return sourceConnectionsStream.collect(Collectors.toList());
    }
}

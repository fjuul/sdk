package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;

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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ActivitySourcesManager {
    @NonNull private final ActivitySourcesManagerConfig config;
    @NonNull private final GoogleFitSyncWorkManager gfSyncWorkManager;
    @NonNull private final ActivitySourcesService sourcesService;
    @NonNull private final ActivitySourcesStateStore stateStore;
    @Nullable private volatile List<TrackerConnection> currentConnections;

    @Nullable private volatile static ActivitySourcesManager instance;

    ActivitySourcesManager(@NonNull ActivitySourcesManagerConfig config,
                           @NonNull GoogleFitSyncWorkManager gfSyncWorkManager,
                           @NonNull ActivitySourcesService sourcesService,
                           @NonNull ActivitySourcesStateStore stateStore,
                           @Nullable List<TrackerConnection> connections) {
        this.config = config;
        this.gfSyncWorkManager = gfSyncWorkManager;
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.currentConnections = connections;
    }

    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client) {
        initialize(client, ActivitySourcesManagerConfig.buildDefault());
    }

    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client,
                                               @NonNull ActivitySourcesManagerConfig config) {
        ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage(), client.getUserToken());
        List<TrackerConnection> storedConnections = stateStore.getConnections().orElse(null);
        ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        WorkManager workManager = WorkManager.getInstance(client.getAppContext());
        GoogleFitActivitySource.initialize(client);
        GoogleFitSyncWorkManager gfSyncWorkManager = new GoogleFitSyncWorkManager(workManager,
            client.getUserToken(),
            client.getUserToken(),
            client.getApiKey(),
            client.getBaseUrl());
        setupBackgroundWorksByConnections(storedConnections, gfSyncWorkManager, config);
        instance = new ActivitySourcesManager(config, gfSyncWorkManager, sourcesService, stateStore, storedConnections);
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

    @SuppressLint("NewApi")
    public void refreshCurrent(@Nullable Callback<List<ActivitySourceConnection>> callback) {
        sourcesService.getCurrentConnections().enqueue((call, apiCallResult) -> {
            if (apiCallResult.isError()) {
                if (callback != null) {
                    callback.onResult(Result.error(apiCallResult.getError()));
                }
                return;
            }
            final List<TrackerConnection> freshTrackerConnections = Arrays.asList(apiCallResult.getValue());
            setupBackgroundWorksByConnections(freshTrackerConnections, gfSyncWorkManager, config);
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

    public static void disableBackgroundGFSyncWorkers(@NonNull Context applicationContext) {
        final WorkManager workManager = WorkManager.getInstance(applicationContext);
        GoogleFitSyncWorkManager.cancelWorks(workManager);
    }

    private static void setupBackgroundWorksByConnections(@Nullable List<TrackerConnection> trackerConnections,
                                                          @NonNull GoogleFitSyncWorkManager gfSyncWorkManager,
                                                          @NonNull ActivitySourcesManagerConfig config) {
        if (checkIfHasGoogleFitConnection(trackerConnections)) {
            configureBackgroundGFSyncWorks(gfSyncWorkManager, config);
        } else {
            gfSyncWorkManager.cancelWorks();
        }
    }

    @SuppressLint("NewApi")
    private static boolean checkIfHasGoogleFitConnection(@Nullable List<TrackerConnection> trackerConnections) {
        return Optional.ofNullable(trackerConnections).flatMap(connections -> {
            return connections.stream()
                .filter(c -> c.getTracker().equals(ActivitySource.TrackerValue.GOOGLE_FIT))
                .findFirst();
        }).isPresent();
    }

    private static void configureBackgroundGFSyncWorks(@NonNull GoogleFitSyncWorkManager gfSyncWorkManager,
                                                        @NonNull ActivitySourcesManagerConfig config) {
        switch (config.getGfIntradayBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelIntradaySyncWork();
                break;
            }
            case ENABLED: {
                gfSyncWorkManager.scheduleIntradaySyncWork(config.getGfIntradayBackgroundSyncMetrics());
                break;
            }
        }
        switch (config.getGfSessionsBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelSessionsSyncWork();
                break;
            }
            case ENABLED: {
                gfSyncWorkManager.scheduleSessionsSyncWork(config.getGfSessionsBackgroundSyncMinSessionDuration());
                break;
            }
        }
    }
}

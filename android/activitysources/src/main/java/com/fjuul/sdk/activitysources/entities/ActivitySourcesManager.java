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

/**
 * The `ActivitySourcesManager` encapsulates connection to fitness trackers, access to current user's tracker connections. This is a high-level entity and entry point of the whole 'activity sources' module.
 * The class designed as the singleton, so you need first to initialize it before getting the instance.
 * For proper initialization you have to provide the configured api-client built with the user credentials.
 */
public final class ActivitySourcesManager {
    @NonNull private final ActivitySourcesManagerConfig config;
    @NonNull private final BackgroundWorkManager backgroundWorkManager;
    @NonNull private final ActivitySourcesService sourcesService;
    @NonNull private final ActivitySourcesStateStore stateStore;
    @Nullable private volatile List<TrackerConnection> currentConnections;

    @Nullable private volatile static ActivitySourcesManager instance;

    ActivitySourcesManager(@NonNull ActivitySourcesManagerConfig config,
                           @NonNull BackgroundWorkManager backgroundWorkManager,
                           @NonNull ActivitySourcesService sourcesService,
                           @NonNull ActivitySourcesStateStore stateStore,
                           @Nullable List<TrackerConnection> connections) {
        this.config = config;
        this.backgroundWorkManager = backgroundWorkManager;
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.currentConnections = connections;
    }

    /**
     * Initialize the singleton with default config. Periodic background works for syncing GoogleFit
     * intraday and session data will be automatically scheduled if a user has a GoogleFit connection.
     * @param client configured client with signing ability and user credentials
     */
    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client) {
        initialize(client, ActivitySourcesManagerConfig.buildDefault());
    }

    /**
     * Initialize the singleton.
     * @param client configured client with signing ability and user credentials
     * @param config config for ActivitySourcesManager
     */
    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client,
                                               @NonNull ActivitySourcesManagerConfig config) {
        final ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage(), client.getUserToken());
        final List<TrackerConnection> storedConnections = stateStore.getConnections().orElse(null);
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final WorkManager workManager = WorkManager.getInstance(client.getAppContext());
        final GoogleFitSyncWorkManager gfSyncWorkManager = new GoogleFitSyncWorkManager(workManager,
            client.getUserToken(),
            client.getUserSecret(),
            client.getApiKey(),
            client.getBaseUrl());
        GoogleFitActivitySource.initialize(client);

        final BackgroundWorkManager backgroundWorkManager = new BackgroundWorkManager(config, gfSyncWorkManager);
        setupBackgroundWorksByConnections(storedConnections, backgroundWorkManager);
        instance = new ActivitySourcesManager(config, backgroundWorkManager, sourcesService, stateStore, storedConnections);
    }

    /**
     * Return previously initialized instance. This method throws IllegalStateException if it is invoked before the initialization.
     * @return instance of ActivitySourcesManager
     * @throws IllegalStateException
     */
    @NonNull
    public static ActivitySourcesManager getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("You must initialize first before getting the instance");
        }
        return instance;
    }

    /**
     * Provides an intent performing connection to the specified ActivitySource.<br>
     * After getting it in the callback, you need to do one of the following:
     * <ul>
     *     <li> if you wanted to connect to GoogleFit tracker, then you need to pass this intent to #startActivityForResult method of Activity or Fragment.
     *     Connection to GoogleFit will show a window prompting all required permissions. A response of user decisions on the prompted window can be retir
     *     Connection to GoogleFit will show a window prompting all required permissions.
     *     <li>if you wanted to connect to any other tracker, then you need to pass this intent to #startActivity method of Activity or Fragment
     * </ul>
     * Connection to GoogleFit will show a window prompting all required permissions.
     * @param activitySource
     * @param callback
     */
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
            setupBackgroundWorksByConnections(freshTrackerConnections, backgroundWorkManager);
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
                                                          @NonNull BackgroundWorkManager backgroundWorkManager) {
        if (checkIfHasGoogleFitConnection(trackerConnections)) {
            backgroundWorkManager.configureBackgroundGFSyncWorks();
        } else {
            backgroundWorkManager.cancelBackgroundGFSyncWorks();
        }
    }

    @SuppressLint("NewApi")
    private static boolean checkIfHasGoogleFitConnection(@Nullable List<TrackerConnection> trackerConnections) {
        return Optional.ofNullable(trackerConnections).flatMap(connections -> {
            return connections.stream()
                .filter(c -> c.getTracker().equals(ActivitySource.TrackerValue.GOOGLE_FIT.getValue()))
                .findFirst();
        }).isPresent();
    }
}

package com.fjuul.sdk.activitysources.entities;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceResolver;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceWorkScheduler;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourcesStateStore;
import com.fjuul.sdk.activitysources.entities.internal.BackgroundWorkManager;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.FjuulException;
import com.fjuul.sdk.core.utils.Logger;
import com.google.android.gms.tasks.Task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;

/**
 * The `ActivitySourcesManager` encapsulates connection to fitness trackers, access to current user's tracker
 * connections. This is a high-level entity and entry point of the whole 'activity sources' module. The class designed
 * as the singleton, so you need first to initialize it before getting the instance. For the proper initialization, you
 * have to provide the configured api-client built with the user credentials.<br>
 * One of the main functions of this module is to connect to activity sources. There are local (i.e. Google Fit) and
 * external trackers (i.e. Polar, Garmin, Fitbit, etc). External trackers require user authentication in the web
 * browser.<br>
 * To use this module properly when connecting to the external activity source, you need to make sure about an Activity
 * from which the connection will be initiated that:
 * <ol>
 * <li>it implements deep linking support. In short, Android Manifest should have an `intent-filter` inside the
 * definition of activity like:
 *
 * <pre>
 * {@code
 *    <intent-filter>
 *      <data android:scheme="YOUR_SCHEME" />
 *      <action android:name="android.intent.action.VIEW" />
 *      <category android:name="android.intent.category.DEFAULT" />
 *      <category android:name="android.intent.category.BROWSABLE" />
 *    </intent-filter>
 * }
 * </pre>
 *
 * where `YOUR_SCHEME` is the scheme provided to you or coordinated with you by Fjuul. For detailed instructions, you
 * can follow the official <a href="https://developer.android.com/training/app-links/deep-linking">guide</a>.</li>
 * <li>it has a `launchMode` declaration with the value `singleTask` or `singleTop` in AndroidManifest to return back
 * from the web browser to the app after the connection is complete.</li>
 * </ol>
 */
public final class ActivitySourcesManager {
    @NonNull
    private final ActivitySourcesManagerConfig config;
    @NonNull
    private final BackgroundWorkManager backgroundWorkManager;
    @NonNull
    private final ActivitySourcesService sourcesService;
    @NonNull
    private final ActivitySourcesStateStore stateStore;
    @NonNull
    private final ActivitySourceResolver activitySourceResolver;
    @NonNull
    private volatile CopyOnWriteArrayList<TrackerConnection> currentConnections;

    @Nullable
    private volatile static ActivitySourcesManager instance;

    ActivitySourcesManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull BackgroundWorkManager backgroundWorkManager,
        @NonNull ActivitySourcesService sourcesService,
        @NonNull ActivitySourcesStateStore stateStore,
        @NonNull ActivitySourceResolver activitySourceResolver,
        @NonNull CopyOnWriteArrayList<TrackerConnection> connections) {
        this.config = config;
        this.backgroundWorkManager = backgroundWorkManager;
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.activitySourceResolver = activitySourceResolver;
        this.currentConnections = connections;
    }

    /**
     * Initialize the singleton with the default config. With the default config:
     * <ul>
     * <li>all fitness metrics will be taken into account;</li>
     * <li>periodic background works for syncing intraday and session data of Google Fit will be automatically scheduled
     * if a user has a current GoogleFit connection.</li>
     * </ul>
     *
     * @param client configured client with signing ability and user credentials
     * @see #initialize(ApiClient, ActivitySourcesManagerConfig)
     */
    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client) {
        initialize(client, ActivitySourcesManagerConfig.buildDefault());
    }


    /**
     * Initialize the singleton with the provided config. <br>
     * Note: make sure that initialization occurs at each start of the user session of your application (most likely, at
     * the start of the application, if the user is already in the system, or after logging in) to avoid using a state
     * of the singleton created by background sync tasks. In other words, do not rely on the logic of checking for the
     * presence of an initialized singleton at the very beginning of the session, initialize ActivitySourcesManager
     * explicitly with the desired configuration.
     *
     * @param client configured client with signing ability and user credentials
     * @param config config for ActivitySourcesManager
     */
    @SuppressLint("NewApi")
    public static synchronized void initialize(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig config) {
        final ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage());
        final List<TrackerConnection> storedConnections = stateStore.getConnections();
        final CopyOnWriteArrayList<TrackerConnection> currentConnections =
            Optional.ofNullable(storedConnections).map(CopyOnWriteArrayList::new).orElse(new CopyOnWriteArrayList<>());
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final WorkManager workManager = WorkManager.getInstance(client.getAppContext());
        final ActivitySourceWorkScheduler scheduler = new ActivitySourceWorkScheduler(workManager,
            client.getUserToken(),
            client.getUserSecret(),
            client.getApiKey(),
            client.getBaseUrl());
        GoogleFitActivitySource.initialize(client, config);
        HealthConnectActivitySource.initialize(client, config, client.getStorage());

        final BackgroundWorkManager backgroundWorkManager = new BackgroundWorkManager(config, scheduler);
        final ActivitySourceResolver activitySourceResolver = new ActivitySourceResolver();
        final ActivitySourcesManager newInstance = new ActivitySourcesManager(config,
            backgroundWorkManager,
            sourcesService,
            stateStore,
            activitySourceResolver,
            currentConnections);
        newInstance.configureExternalStateByConnections(currentConnections);

        instance = newInstance;
        Logger.get().d("initialized successfully (the previous one could be overridden)");
    }

    /**
     * Return the previously initialized instance. This method throws IllegalStateException if it is invoked before the
     * initialization.
     *
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
     * <li>if you wanted to connect to the GoogleFit tracker, then you need to pass this intent to
     * #startActivityForResult method of Activity or Fragment. Connection to GoogleFit will show a window prompting all
     * required GoogleFit OAuth permissions. A response of user decisions on the prompted window will be available in
     * #onActivityResult method of Activity or Fragment with the `requestCode` you specified to
     * #startActivityForResult.<br>
     * After you compared `requestCode` with your and checked `resultCode` with `Activity.RESULT_OK`, you need to pass
     * the coming `data` (Intent) in #onActivityResult to {@link GoogleFitActivitySource#handleGoogleSignInResult}
     * method to complete the connection to the GoogleFit tracker.
     *
     * <pre>
     *     {@code
     *     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     *       super.onActivityResult(requestCode, resultCode, data)
     *       if (requestCode == YOUR_REQUEST_CODE) {
     *         if (resultCode == Activity.RESULT_OK && data != null) {
     *           GoogleFitActivitySource.getInstance().handleGoogleSignInResult(data) { result ->
     *             if (result.isError) {
     *               // get the error via result.getError()
     *             }
     *             // successfully connected
     *           }
     *         } else {
     *             // the prompted window was declined
     *         }
     *       }
     *     }
     *     }
     * </pre>
     *
     * <li>if you wanted to connect to any other tracker, then you need to pass this intent to #startActivity method of
     * Activity or Fragment. This will open the user's web browser on the page with authorization of the specified
     * tracker. After the user successfully authenticates, the user will be redirected back to the app by the link
     * matched with the scheme provided to you or coordinated with you by Fjuul.<br>
     * Returning to the app and processing the connection result is available in the #onNewIntent method of your
     * Activity class, which contains an intent-filter with the SDK schema. There you can determine which schema the
     * incoming link belongs to and find out the result of the external connection:
     *
     * <pre>
     *     {@code
     *     override fun onNewIntent(intent: Intent?) {
     *       super.onNewIntent(intent)
     *       if (intent?.data?.scheme == "your-fjuulsdk-sceme") {
     *         val redirectResult = ExternalAuthenticationFlowHandler.handle(intent.data!!)
     *         if (redirectResult != null && redirectResult.isSuccess) {
     *           // successfully connected to the external tracker
     *         }
     *       }
     *     }
     *     }
     * </pre>
     * </ul>
     * One important remark: Many functional parts of ActivitySourcesManager depend on the list of current connections.
     * Because of the complexity of the connection flow, there is not an automatic refreshing of the user's connection
     * list. Therefore, after a user succeeds in the connection, please invoke refreshing current connections of the
     * user via the {@link #refreshCurrent} method.
     *
     * @see ActivitySourcesManager#refreshCurrent
     * @see ExternalAuthenticationFlowHandler
     * @see GoogleFitActivitySource
     * @param activitySource instance of ActivitySource to connect
     * @param callback callback bringing the connecting intent
     */
    public void connect(@NonNull final ActivitySource activitySource, @NonNull final Callback<Intent> callback) {
        if (activitySource instanceof GoogleFitActivitySource) {
            final Intent intent = ((GoogleFitActivitySource) activitySource).buildIntentRequestingFitnessPermissions();
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


    /**
     * Disconnects the activity source connection and removes the connection from the current list. In the case of
     * GoogleFitActivitySource, this will revoke GoogleFit OAuth permissions.
     *
     * @param sourceConnection current connection to disconnect
     * @param callback callback bringing the operation result
     */
    @SuppressLint("NewApi")
    public void disconnect(@NonNull final ActivitySourceConnection sourceConnection,
        @NonNull final Callback<Void> callback) {
        // TODO: validate if sourceConnection was already ended ?
        final Runnable runnableDisconnect = () -> {
            sourcesService.disconnect(sourceConnection).enqueue((call, apiCallResult) -> {
                if (apiCallResult.isError()) {
                    if (callback != null) {
                        callback.onResult(Result.error(apiCallResult.getError()));
                    }
                    return;
                }
                final TrackerConnection connectionToRemove = this.currentConnections.stream()
                    .filter(connection -> connection.getId().equals(sourceConnection.getId()))
                    .findFirst()
                    .orElse(null);
                this.currentConnections.remove(connectionToRemove);
                this.stateStore.setConnections(this.currentConnections);
                this.configureExternalStateByConnections(currentConnections);
                callback.onResult(Result.value(null));
            });
        };
        final ActivitySource activitySource = sourceConnection.getActivitySource();
        if (activitySource instanceof GoogleFitActivitySource) {
            final Task<Void> disableGoogleFitTask = ((GoogleFitActivitySource) activitySource).disable();
            // NOTE: callback of `addOnCompleteListener` will be executed on main thread since there is not provided
            // executor for it.
            // TODO: introduce own executor for ActivitySourcesManager to run all internal operations on it
            disableGoogleFitTask.addOnCompleteListener((task) -> {
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

    /**
     * Returns a list of current connections of the user. The list with current user connections is automatically saved
     * in the persistent storage. If there isn't yet the persisted state of the current connections of the user then
     * this method returns an empty list.<br>
     * NOTE: Although you can use singleton instances of the ActivitySource classes directly, it is recommended that you
     * should use this method to work with their instances (for example, GoogleFitActivitySource) by getting them with
     * the {@link ActivitySourceConnection#getActivitySource()} method.
     *
     * @see #refreshCurrent(Callback)
     * @return list of activity source connections
     */
    @SuppressLint("NewApi")
    @NonNull
    public List<ActivitySourceConnection> getCurrent() {
        return convertTrackerConnectionsToActivitySourcesConnections(activitySourceResolver, this.currentConnections);
    }

    /**
     * Requests for the fresh state of user tracker connections.
     *
     * @param callback callback bringing the updated connection list
     */
    @SuppressLint("NewApi")
    public void refreshCurrent(@Nullable Callback<List<ActivitySourceConnection>> callback) {
        sourcesService.getCurrentConnections().enqueue((call, apiCallResult) -> {
            if (apiCallResult.isError()) {
                if (callback != null) {
                    callback.onResult(Result.error(apiCallResult.getError()));
                }
                return;
            }
            final CopyOnWriteArrayList<TrackerConnection> freshTrackerConnections =
                new CopyOnWriteArrayList(apiCallResult.getValue());
            configureExternalStateByConnections(freshTrackerConnections);
            stateStore.setConnections(freshTrackerConnections);
            this.currentConnections = freshTrackerConnections;
            if (callback != null) {
                final List<ActivitySourceConnection> sourceConnections =
                    convertTrackerConnectionsToActivitySourcesConnections(activitySourceResolver,
                        freshTrackerConnections);
                callback.onResult(Result.value(sourceConnections));
            }
        });
    }

    /**
     * Cancel any scheduled background works initiated by the user state and {@link ActivitySourcesManagerConfig}
     * before.
     *
     * @param applicationContext
     */
    public static void disableBackgroundWorkers(@NonNull Context applicationContext) {
        final WorkManager workManager = WorkManager.getInstance(applicationContext);
        ActivitySourceWorkScheduler.cancelWorks(workManager);
    }

    @SuppressLint("NewApi")
    private static List<ActivitySourceConnection> convertTrackerConnectionsToActivitySourcesConnections(
        @NonNull ActivitySourceResolver activitySourceResolver,
        @NonNull List<TrackerConnection> trackerConnections) {
        final Stream<ActivitySourceConnection> sourceConnectionsStream = trackerConnections.stream().map(connection -> {
            final ActivitySource activitySource =
                activitySourceResolver.getInstanceByTrackerValue(connection.getTracker());
            return new ActivitySourceConnection(connection, activitySource);
        });
        return sourceConnectionsStream.collect(Collectors.toList());
    }

    @SuppressLint("NewApi")
    private void configureExternalStateByConnections(@Nullable List<TrackerConnection> trackerConnections) {
        configureGoogleFitState(trackerConnections);
        configureHealthConnectState(trackerConnections);
    }

    private void configureGoogleFitState(@Nullable List<TrackerConnection> trackerConnections) {
        final TrackerConnection gfTrackerConnection = Optional.ofNullable(trackerConnections)
            .flatMap(connections -> connections.stream()
                .filter(c -> c.getTracker().equals(TrackerValue.GOOGLE_FIT.getValue()))
                .findFirst())
            .orElse(null);

        if (gfTrackerConnection != null) {
            backgroundWorkManager.configureProfileSyncWork();
            backgroundWorkManager.configureGFSyncWorks();
        } else {
            backgroundWorkManager.cancelGFSyncWorks();
            backgroundWorkManager.cancelProfileSyncWork();
        }

        final GoogleFitActivitySource googleFit = (GoogleFitActivitySource) activitySourceResolver
            .getInstanceByTrackerValue(TrackerValue.GOOGLE_FIT.getValue());
        if (gfTrackerConnection != null) {
            googleFit.setLowerDateBoundary(gfTrackerConnection.getCreatedAt());
        } else {
            googleFit.setLowerDateBoundary(null);
        }
    }

    private void configureHealthConnectState(@Nullable List<TrackerConnection> trackerConnections) {
        final TrackerConnection hcTrackerConnection = Optional.ofNullable(trackerConnections)
            .flatMap(connections -> connections.stream()
                .filter(c -> c.getTracker().equals(TrackerValue.HEALTH_CONNECT.getValue()))
                .findFirst())
            .orElse(null);

        if (hcTrackerConnection != null) {
            backgroundWorkManager.configureHCProfileSyncWork();
            backgroundWorkManager.configureHCIntradaySyncWorks();
            backgroundWorkManager.configureHCDailySyncWorks();
        } else {
            backgroundWorkManager.cancelHCIntradaySyncWorks();
            backgroundWorkManager.cancelHCDailySyncWorks();
            backgroundWorkManager.cancelHCProfileSyncWork();
        }

        final HealthConnectActivitySource healthConnect = (HealthConnectActivitySource) activitySourceResolver
            .getInstanceByTrackerValue(TrackerValue.HEALTH_CONNECT.getValue());
        if (hcTrackerConnection != null) {
            healthConnect.setLowerDateBoundary(hcTrackerConnection.getCreatedAt());
        } else {
            healthConnect.setLowerDateBoundary(null);
        }
    }
}

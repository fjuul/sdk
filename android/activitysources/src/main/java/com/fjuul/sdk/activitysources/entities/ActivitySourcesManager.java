package com.fjuul.sdk.activitysources.entities;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceResolver;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceWorkScheduler;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourcesStateStore;
import com.fjuul.sdk.activitysources.entities.internal.BackgroundWorkManager;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.exceptions.FjuulException;
import com.fjuul.sdk.core.utils.Logger;
import com.google.android.gms.tasks.Task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.work.Data;
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
 * <p>
 * where `YOUR_SCHEME` is the scheme provided to you or coordinated with you by Fjuul. For detailed instructions, you
 * can follow the official <a href="https://developer.android.com/training/app-links/deep-linking">guide</a>.</li>
 * <li>it has a `launchMode` declaration with the value `singleTask` or `singleTop` in AndroidManifest to return back
 * from the web browser to the app after the connection is complete.</li>
 * </ol>
 */
public final class ActivitySourcesManager {
    private static final long REFRESH_AFTER_REJECTED_UPLOAD_TIMEOUT_SECONDS = 30;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService WORK_MANAGER_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "Fjuul-ActivitySources-WorkManager");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private final ActivitySourcesManagerConfig config;
    @NonNull
    private final Context applicationContext;
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
    // The session whose credentials this instance's services and sources use; null only in tests.
    @Nullable
    private final ApiClient client;

    // Requests changing local state are numbered in call order and local state only moves forward: a refresh response
    // is applied only if its request is newer than the last refresh, disconnect or disabling of background works.
    // Guarded by `this`, like `replaced`.
    private long lastSequenceNumber = 0;
    private long appliedSequenceNumber = 0;
    private long lastRefreshAppliedSequenceNumber = 0;
    // Set once `initialize` replaced this instance, so it no longer touches the shared background works and sources.
    private boolean replaced = false;
    private boolean automaticRefreshesEnabled = true;

    @Nullable
    private volatile static ActivitySourcesManager instance;
    @Nullable
    private static Future<?> pendingWorkManagerOperation;

    ActivitySourcesManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull BackgroundWorkManager backgroundWorkManager,
        @NonNull ActivitySourcesService sourcesService,
        @NonNull ActivitySourcesStateStore stateStore,
        @NonNull ActivitySourceResolver activitySourceResolver,
        @NonNull CopyOnWriteArrayList<TrackerConnection> connections,
        @NonNull Context applicationContext) {
        this(config,
            backgroundWorkManager,
            sourcesService,
            stateStore,
            activitySourceResolver,
            connections,
            applicationContext,
            null);
    }

    ActivitySourcesManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull BackgroundWorkManager backgroundWorkManager,
        @NonNull ActivitySourcesService sourcesService,
        @NonNull ActivitySourcesStateStore stateStore,
        @NonNull ActivitySourceResolver activitySourceResolver,
        @NonNull CopyOnWriteArrayList<TrackerConnection> connections,
        @NonNull Context applicationContext,
        @Nullable ApiClient client) {
        this.applicationContext = applicationContext;
        this.config = config;
        this.backgroundWorkManager = backgroundWorkManager;
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.activitySourceResolver = activitySourceResolver;
        this.currentConnections = connections;
        this.client = client;
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
    @WorkerThread
    public static void initialize(@NonNull ApiClient client) {
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
     * @implNote Call from a worker thread; use
     *           {@link #initializeAsync(ApiClient, ActivitySourcesManagerConfig, Callback)} from UI code.
     * @implNote Periodic jobs created by SDK versions before rollback snapshots were introduced cannot be restored if
     *           replacement initialization fails. Their automatic sync may pause until a later initialization with a
     *           config that explicitly configures those jobs; this does not clear source data or sync cursors.
     * @see #initializeAsync(ApiClient, ActivitySourcesManagerConfig, Callback)
     */
    @SuppressLint("NewApi")
    @WorkerThread
    public static void initialize(@NonNull ApiClient client, @NonNull ActivitySourcesManagerConfig config) {
        awaitInitialization(submitWorkManagerTask(() -> initializeInternal(client, config)));
    }

    /** Initializes the manager without blocking the calling thread. Completion is delivered on the main thread. */
    @SuppressLint("NewApi")
    public static void initializeAsync(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig config,
        @NonNull Callback<Void> callback) {
        submitWorkManagerTask(() -> {
            Result<Void> result;
            try {
                initializeInternal(client, config);
                result = Result.value(null);
            } catch (Throwable error) {
                result = Result.error(error);
            }
            deliverOnMain(callback, result);
        });
    }

    private static void initializeInternal(@NonNull ApiClient client, @NonNull ActivitySourcesManagerConfig config) {
        final Context context = client.getAppContext();
        initializeInternal(client,
            config,
            WorkManager.getInstance(context),
            HealthConnectActivitySource.getHealthConnectAvailability(context));
    }

    private static void initializeInternal(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig config,
        @NonNull WorkManager workManager,
        @NonNull HealthConnectAvailability healthConnectAvailability) {
        final ActivitySourcesManager previousInstance = instance;
        final ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage());
        final List<TrackerConnection> storedConnections = stateStore.getConnections();
        final CopyOnWriteArrayList<TrackerConnection> currentConnections =
            Optional.ofNullable(storedConnections).map(CopyOnWriteArrayList::new).orElse(new CopyOnWriteArrayList<>());
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        // Published only once the new instance is set up, so a failure leaves the previous session's sources in place.
        final GoogleFitActivitySource googleFit = GoogleFitActivitySource.create(client, config);
        final HealthConnectActivitySource healthConnect =
            HealthConnectActivitySource.forInternalUseOnly_create(client, config, client.getStorage());
        boolean published = false;
        try {
            final Context context = client.getAppContext();
            final ActivitySourceWorkScheduler scheduler = new ActivitySourceWorkScheduler(workManager,
                client.getUserToken(),
                client.getUserSecret(),
                client.getApiKey(),
                client.getBaseUrl(),
                context);
            final BackgroundWorkManager backgroundWorkManager = new BackgroundWorkManager(config, scheduler);
            final ActivitySourceResolver activitySourceResolver =
                new SessionActivitySourceResolver(googleFit, healthConnect);
            final ActivitySourcesManager newInstance = new ActivitySourcesManager(config,
                backgroundWorkManager,
                sourcesService,
                stateStore,
                activitySourceResolver,
                currentConnections,
                context,
                client);
            final List<ActivitySourceWorkScheduler.ScheduledWork> previousScheduledWorks =
                ActivitySourceWorkScheduler.snapshotWorks(workManager, context);
            try {
                newInstance.configureBackgroundWorkByConnections(currentConnections, healthConnectAvailability);
                newInstance.setSourceDateBoundariesByConnections(currentConnections);
            } catch (RuntimeException e) {
                restoreBackgroundWorks(workManager, context, previousScheduledWorks);
                throw e;
            }
            synchronized (previousInstance != null ? previousInstance : ActivitySourcesManager.class) {
                GoogleFitActivitySource.publish(googleFit);
                HealthConnectActivitySource.forInternalUseOnly_publish(healthConnect);
                if (previousInstance != null) {
                    previousInstance.markReplaced();
                }
                instance = newInstance;
                published = true;
            }
        } finally {
            if (!published) {
                healthConnect.close();
            }
        }
        Logger.get().d("initialized successfully (the previous one could be overridden)");
    }

    @VisibleForTesting
    static void initializeForTesting(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig config,
        @NonNull WorkManager workManager,
        @NonNull HealthConnectAvailability healthConnectAvailability) {
        initializeInternal(client, config, workManager, healthConnectAvailability);
    }

    // Undoes any shared work changes before the candidate session is discarded.
    private static void restoreBackgroundWorks(@NonNull WorkManager workManager,
        @NonNull Context applicationContext,
        @NonNull List<ActivitySourceWorkScheduler.ScheduledWork> previousScheduledWorks) {
        try {
            ActivitySourceWorkScheduler.restoreWorks(workManager, applicationContext, previousScheduledWorks);
        } catch (RuntimeException e) {
            Logger.get().e(e, "failed to restore background works after a failed initialization");
        }
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

    @VisibleForTesting
    static void setInstance(@Nullable ActivitySourcesManager manager) {
        instance = manager;
    }

    /**
     * For the SDK's background workers only: returns the instance if it belongs to the session of the work's
     * credentials, initializes it with them if there is none, or returns null if it belongs to another session.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public static ActivitySourcesManager getOrInitializeForWorker(@NonNull Context applicationContext,
        @NonNull Data inputData) {
        final String userToken = getInputString(inputData, GFSyncWorker.KEY_USER_TOKEN_ARG);
        final String userSecret = getInputString(inputData, GFSyncWorker.KEY_USER_SECRET_ARG);
        final String apiKey = getInputString(inputData, GFSyncWorker.KEY_API_KEY_ARG);
        final String baseUrl = getInputString(inputData, GFSyncWorker.KEY_BASE_URL_ARG);
        while (true) {
            Future<?> pending;
            boolean submittedInitialization = false;
            synchronized (ActivitySourcesManager.class) {
                pending = pendingWorkManagerOperation;
                if (pending == null || pending.isDone()) {
                    final ActivitySourcesManager currentInstance = instance;
                    if (currentInstance != null) {
                        if (currentInstance.belongsToSession(userToken, userSecret, apiKey, baseUrl)) {
                            return currentInstance;
                        }
                        Logger.get().d("skipping background work queued by another session");
                        return null;
                    }
                    submitWorkManagerTask(
                        () -> initializeWorkerSession(applicationContext, userToken, userSecret, apiKey, baseUrl));
                    pending = pendingWorkManagerOperation;
                    submittedInitialization = true;
                }
            }
            if (submittedInitialization) {
                awaitInitialization(pending);
            } else {
                awaitPendingWorkManagerOperation(pending);
            }
        }
    }

    private static void initializeWorkerSession(@NonNull Context applicationContext,
        @NonNull String userToken,
        @NonNull String userSecret,
        @NonNull String apiKey,
        @NonNull String baseUrl) {
        final ApiClient client = new ApiClient.Builder(applicationContext, baseUrl, apiKey)
            .setUserCredentials(new UserCredentials(userToken, userSecret))
            .build();
        initializeInternal(client, workerInitializationConfig());
    }

    @VisibleForTesting
    static ActivitySourcesManagerConfig workerInitializationConfig() {
        return new ActivitySourcesManagerConfig.Builder().keepUntouchedBackgroundSync()
            .setCollectableFitnessMetrics(Collections.emptySet())
            .build();
    }

    private static void awaitPendingWorkManagerOperation(@NonNull Future<?> pendingOperation) {
        try {
            pendingOperation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for activity source work", e);
        } catch (ExecutionException e) {
            Logger.get().e(e.getCause(), "activity source background operation failed");
        }
    }

    @NonNull
    private static String getInputString(@NonNull Data inputData, @NonNull String key) {
        final String value = inputData.getString(key);
        return value != null ? value : "";
    }

    // The secret is compared too, as it issues the keys signing this instance's requests.
    private boolean belongsToSession(@NonNull String userToken,
        @NonNull String userSecret,
        @NonNull String apiKey,
        @NonNull String baseUrl) {
        return client != null
            && client.getUserToken().equals(userToken)
            && client.getUserSecret().equals(userSecret)
            && client.getApiKey().equals(apiKey)
            && client.getBaseUrl().equals(baseUrl);
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
     * @param activitySource instance of ActivitySource to connect
     * @param callback callback bringing the connecting intent
     * @see ActivitySourcesManager#refreshCurrent
     * @see ExternalAuthenticationFlowHandler
     * @see GoogleFitActivitySource
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
            if (activitySource instanceof HealthConnectActivitySource) {
                final HealthConnectAvailability healthConnectAvailability =
                    HealthConnectActivitySource.getHealthConnectAvailability(applicationContext);
                if (healthConnectAvailability == HealthConnectAvailability.SDK_AVAILABLE) {
                    final Intent intent = new Intent();
                    callback.onResult(Result.value(intent));
                } else {
                    if (healthConnectAvailability == HealthConnectAvailability.SDK_UNAVAILABLE) {
                        callback.onResult(Result.error(new FjuulException("Health Connect not supported")));
                    } else
                        if (healthConnectAvailability == HealthConnectAvailability.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                            callback.onResult(Result.error(
                                new FjuulException("Health Connect is either not installed or needs to be updated")));
                        }
                }

            } else if (connectionResult instanceof ExternalAuthenticationFlowRequired) {
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
                    deliverOnMain(callback, Result.error(apiCallResult.getError()));
                    return;
                }
                final long disconnectSequenceNumber;
                final boolean managerWasReplaced;
                synchronized (this) {
                    disconnectSequenceNumber = nextSequenceNumber();
                    appliedSequenceNumber = disconnectSequenceNumber;
                    managerWasReplaced = replaced;
                }
                HealthConnectAvailability availability = null;
                RuntimeException availabilityError = null;
                if (!managerWasReplaced) {
                    try {
                        availability = HealthConnectActivitySource.getHealthConnectAvailability(applicationContext);
                    } catch (RuntimeException error) {
                        availabilityError = error;
                    }
                }
                final HealthConnectAvailability healthConnectAvailability = availability;
                final RuntimeException capturedAvailabilityError = availabilityError;
                executeWorkManagerTask(() -> {
                    try {
                        final CopyOnWriteArrayList<TrackerConnection> updatedConnections;
                        final boolean updateExternalState;
                        final boolean applyDisconnect;
                        synchronized (this) {
                            applyDisconnect = lastRefreshAppliedSequenceNumber <= disconnectSequenceNumber;
                            updatedConnections = new CopyOnWriteArrayList<>(this.currentConnections);
                            updatedConnections
                                .removeIf(connection -> connection.getId().equals(sourceConnection.getId()));
                            updateExternalState = applyDisconnect && !replaced;
                        }
                        if (!applyDisconnect) {
                            deliverOnMain(callback, Result.value(null));
                            return;
                        }
                        stateStore.setConnections(updatedConnections);
                        synchronized (this) {
                            this.currentConnections = updatedConnections;
                        }
                        setSourceDateBoundariesByConnections(updatedConnections);
                        if (updateExternalState) {
                            if (capturedAvailabilityError != null) {
                                throw capturedAvailabilityError;
                            }
                            configureBackgroundWorkByConnections(updatedConnections, healthConnectAvailability);
                        }
                        deliverOnMain(callback, Result.value(null));
                    } catch (Throwable error) {
                        deliverOnMain(callback, Result.error(error));
                    }
                });
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
                    deliverOnMain(callback, Result.error(task.getException()));
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
     * @return list of activity source connections
     * @see #refreshCurrent(Callback)
     */
    @SuppressLint("NewApi")
    @NonNull
    public List<ActivitySourceConnection> getCurrent() {
        return convertTrackerConnectionsToActivitySourcesConnections(activitySourceResolver, this.currentConnections);
    }

    /**
     * Requests for the fresh state of user tracker connections. The fresh connections are only applied locally if no
     * later refresh has been applied and no disconnect or {@link #disableBackgroundWorkers} happened since the request
     * was made; the callback brings them either way.
     *
     * @param callback callback bringing the updated connection list
     */
    @SuppressLint("NewApi")
    public void refreshCurrent(@Nullable Callback<List<ActivitySourceConnection>> callback) {
        final long sequenceNumber = nextSequenceNumber();
        sourcesService.getCurrentConnections().enqueue((call, apiCallResult) -> {
            if (apiCallResult.isError()) {
                deliverOnMain(callback, Result.error(apiCallResult.getError()));
                return;
            }
            final CopyOnWriteArrayList<TrackerConnection> freshTrackerConnections =
                new CopyOnWriteArrayList(apiCallResult.getValue());
            final HealthConnectAvailability healthConnectAvailability;
            try {
                healthConnectAvailability =
                    HealthConnectActivitySource.getHealthConnectAvailability(applicationContext);
            } catch (RuntimeException error) {
                deliverOnMain(callback, Result.error(error));
                return;
            }
            executeWorkManagerTask(() -> {
                final Result<List<ActivitySourceConnection>> result;
                try {
                    final boolean applyRefresh;
                    synchronized (this) {
                        // The response may predate a newer refresh, a disconnect, a logout or a re-initialization.
                        applyRefresh = !replaced && sequenceNumber > appliedSequenceNumber;
                        if (applyRefresh) {
                            appliedSequenceNumber = sequenceNumber;
                        }
                    }
                    if (applyRefresh) {
                        configureBackgroundWorkByConnections(freshTrackerConnections, healthConnectAvailability);
                        final boolean commitRefresh;
                        synchronized (this) {
                            commitRefresh = !replaced && sequenceNumber == appliedSequenceNumber;
                            if (commitRefresh) {
                                setSourceDateBoundariesByConnections(freshTrackerConnections);
                                this.currentConnections = freshTrackerConnections;
                                lastRefreshAppliedSequenceNumber = sequenceNumber;
                            }
                        }
                        if (commitRefresh) {
                            stateStore.setConnections(freshTrackerConnections);
                        }
                    }
                    final List<ActivitySourceConnection> sourceConnections =
                        convertTrackerConnectionsToActivitySourcesConnections(activitySourceResolver,
                            freshTrackerConnections);
                    result = Result.value(sourceConnections);
                } catch (Throwable error) {
                    deliverOnMain(callback, Result.error(error));
                    return;
                }
                deliverOnMain(callback, result);
            });
        });
    }

    // The backend responds to uploads with 409 when it has no current connection to the data source.
    // Waits for the refresh so a background worker can't finish (and the process die) before it completes.
    static void refreshCurrentIfUploadRejected(@Nullable Throwable uploadError) {
        refreshCurrentIfUploadRejected(uploadError, instance);
    }

    @Nullable
    static ActivitySourcesManager managerForSync(@NonNull ActivitySource source) {
        final ActivitySourcesManager manager = instance;
        return manager != null
            && manager.activitySourceResolver.getInstanceByTrackerValue(source.getTrackerValue().getValue()) == source
                ? manager
                : null;
    }

    static void refreshCurrentIfUploadRejected(@Nullable Throwable uploadError,
        @Nullable ActivitySourcesManager manager) {
        if (manager == null || !isCausedByConflict(uploadError)) {
            return;
        }
        final CountDownLatch refreshed = new CountDownLatch(1);
        synchronized (manager) {
            if (manager.replaced || !manager.automaticRefreshesEnabled) {
                return;
            }
            Logger.get().d("upload rejected with HTTP 409, refreshing current connections");
            manager.refreshCurrent(result -> refreshed.countDown());
        }
        // The refresh callback is delivered on the main thread, so waiting there would deadlock.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }
        try {
            refreshed.await(REFRESH_AFTER_REJECTED_UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isCausedByConflict(@Nullable Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiExceptions.ConflictException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel any scheduled background works initiated by the user state and {@link ActivitySourcesManagerConfig}
     * before.
     *
     * @param applicationContext
     */
    public static void disableBackgroundWorkers(@NonNull Context applicationContext) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            disableBackgroundWorkersAsync(applicationContext, null);
        } else {
            awaitInitialization(enqueueBackgroundCancellation(applicationContext, null));
        }
    }

    /** Cancels SDK background work without blocking the caller. Completion is delivered on the main thread. */
    public static synchronized void disableBackgroundWorkersAsync(@NonNull Context applicationContext,
        @Nullable Callback<Void> callback) {
        enqueueBackgroundCancellation(applicationContext, callback);
    }

    private static synchronized Future<?> enqueueBackgroundCancellation(@NonNull Context applicationContext,
        @Nullable Callback<Void> callback) {
        final ActivitySourcesManager requestedInstance = instance;
        invalidateAutomaticRefreshes(requestedInstance);
        executeWorkManagerTask(() -> {
            final ActivitySourcesManager activeInstance = instance;
            if (activeInstance != requestedInstance) {
                invalidateAutomaticRefreshes(activeInstance);
            }
            try {
                ActivitySourceWorkScheduler.cancelWorks(WorkManager.getInstance(applicationContext),
                    applicationContext);
                deliverOnMain(callback, Result.value(null));
            } catch (RuntimeException e) {
                Logger.get().e(e, "failed to cancel activity source background works");
                deliverOnMain(callback, Result.error(e));
                if (callback == null) {
                    throw e;
                }
            }
        });
        return pendingWorkManagerOperation;
    }

    private static void invalidateAutomaticRefreshes(@Nullable ActivitySourcesManager manager) {
        if (manager == null) {
            return;
        }
        synchronized (manager) {
            manager.automaticRefreshesEnabled = false;
            manager.supersedeEarlierRefreshes();
        }
    }

    private static void executeWorkManagerTask(@NonNull Runnable task) {
        submitWorkManagerTask(task);
    }

    private static synchronized Future<?> submitWorkManagerTask(@NonNull Runnable task) {
        pendingWorkManagerOperation = WORK_MANAGER_EXECUTOR.submit(task);
        return pendingWorkManagerOperation;
    }

    @VisibleForTesting
    static Future<?> enqueueWorkManagerTaskForTesting(@NonNull Runnable task) {
        return submitWorkManagerTask(task);
    }

    private static void awaitInitialization(@NonNull Future<?> operation) {
        try {
            operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for activity source work", e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Activity source operation failed", cause);
        }
    }

    @VisibleForTesting
    static void awaitWorkManagerExecutorForTesting() {
        final CountDownLatch completed = new CountDownLatch(1);
        submitWorkManagerTask(completed::countDown);
        try {
            if (!completed.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("WorkManager executor did not become idle");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static <T> void deliverOnMain(@Nullable Callback<T> callback, @NonNull Result<T> result) {
        if (callback == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onResult(result);
        } else {
            MAIN_HANDLER.post(() -> callback.onResult(result));
        }
    }

    private synchronized long nextSequenceNumber() {
        return ++lastSequenceNumber;
    }

    synchronized void supersedeEarlierRefreshes() {
        appliedSequenceNumber = nextSequenceNumber();
    }

    synchronized void markReplaced() {
        replaced = true;
    }

    synchronized boolean automaticRefreshesEnabledForTesting() {
        return automaticRefreshesEnabled;
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

    private void configureBackgroundWorkByConnections(@Nullable List<TrackerConnection> trackerConnections,
        @NonNull HealthConnectAvailability healthConnectAvailability) {
        configureGoogleFitBackgroundWork(trackerConnections);
        configureHealthConnectBackgroundWork(trackerConnections, healthConnectAvailability);
    }

    private void setSourceDateBoundariesByConnections(@Nullable List<TrackerConnection> trackerConnections) {
        final TrackerConnection gfTrackerConnection =
            findConnection(trackerConnections, TrackerValue.GOOGLE_FIT.getValue());
        final GoogleFitActivitySource googleFit = (GoogleFitActivitySource) activitySourceResolver
            .getInstanceByTrackerValue(TrackerValue.GOOGLE_FIT.getValue());
        googleFit.setLowerDateBoundary(gfTrackerConnection == null ? null : gfTrackerConnection.getCreatedAt());

        final TrackerConnection hcTrackerConnection =
            findConnection(trackerConnections, TrackerValue.HEALTH_CONNECT.getValue());
        final HealthConnectActivitySource healthConnect = (HealthConnectActivitySource) activitySourceResolver
            .getInstanceByTrackerValue(TrackerValue.HEALTH_CONNECT.getValue());
        healthConnect.setLowerDateBoundary(hcTrackerConnection == null ? null : hcTrackerConnection.getCreatedAt());
    }

    @Nullable
    private static TrackerConnection findConnection(@Nullable List<TrackerConnection> trackerConnections,
        @NonNull String trackerValue) {
        return Optional.ofNullable(trackerConnections)
            .flatMap(connections -> connections.stream().filter(c -> c.getTracker().equals(trackerValue)).findFirst())
            .orElse(null);
    }

    private void configureGoogleFitBackgroundWork(@Nullable List<TrackerConnection> trackerConnections) {
        final TrackerConnection gfTrackerConnection = Optional.ofNullable(trackerConnections)
            .flatMap(connections -> connections.stream()
                .filter(c -> c.getTracker().equals(TrackerValue.GOOGLE_FIT.getValue()))
                .findFirst())
            .orElse(null);

        if (gfTrackerConnection != null) {
            backgroundWorkManager.configureProfileSyncWork();
            backgroundWorkManager.configureGFSyncWorks();
        } else {
            if (config
                .getGoogleFitIntradayBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelGFIntradaySyncWork();
            }
            if (config
                .getGoogleFitSessionsBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelGFSessionsSyncWork();
            }
            if (config.getProfileBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelProfileSyncWork();
            }
        }

    }

    private void configureHealthConnectBackgroundWork(@Nullable List<TrackerConnection> trackerConnections,
        @NonNull HealthConnectAvailability healthConnectAvailability) {
        final TrackerConnection hcTrackerConnection = Optional.ofNullable(trackerConnections)
            .flatMap(connections -> connections.stream()
                .filter(c -> c.getTracker().equals(TrackerValue.HEALTH_CONNECT.getValue()))
                .findFirst())
            .orElse(null);

        if (hcTrackerConnection != null && healthConnectAvailability == HealthConnectAvailability.SDK_AVAILABLE) {
            backgroundWorkManager.configureHCProfileSyncWork();
            backgroundWorkManager.configureHCIntradaySyncWorks();
            backgroundWorkManager.configureHCDailySyncWorks();
        } else {
            if (config
                .getHealthConnectIntradayBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelHCIntradaySyncWorks();
            }
            if (config
                .getHealthConnectDailyBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelHCDailySyncWorks();
            }
            if (config
                .getHealthConnectProfileBackgroundSyncMode() != ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED) {
                backgroundWorkManager.cancelHCProfileSyncWork();
            }
        }

    }

    // Resolves the session-bound sources this instance was created with rather than the published singletons, which
    // a later initialization replaces.
    private static final class SessionActivitySourceResolver extends ActivitySourceResolver {
        @NonNull
        private final GoogleFitActivitySource googleFit;
        @NonNull
        private final HealthConnectActivitySource healthConnect;

        SessionActivitySourceResolver(@NonNull GoogleFitActivitySource googleFit,
            @NonNull HealthConnectActivitySource healthConnect) {
            this.googleFit = googleFit;
            this.healthConnect = healthConnect;
        }

        @NonNull
        @Override
        public ActivitySource getInstanceByTrackerValue(@NonNull String trackerValue) {
            final TrackerValue tracker = TrackerValue.forValue(trackerValue);
            if (TrackerValue.GOOGLE_FIT.equals(tracker)) {
                return googleFit;
            } else if (TrackerValue.HEALTH_CONNECT.equals(tracker)) {
                return healthConnect;
            }
            return super.getInstanceByTrackerValue(trackerValue);
        }
    }
}

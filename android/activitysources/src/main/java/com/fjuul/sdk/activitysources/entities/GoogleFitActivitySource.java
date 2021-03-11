package com.fjuul.sdk.activitysources.entities;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.activitysources.entities.internal.GFDataManager;
import com.fjuul.sdk.activitysources.entities.internal.GFDataManagerBuilder;
import com.fjuul.sdk.activitysources.entities.internal.GFDataUtils;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.ActivityRecognitionPermissionNotGrantedException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.FjuulException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * The ActivitySource class for the GoogleFit tracker. This is a local activity source (i.e. data that needs to be
 * synced will be taken from the user device).
 */
@SuppressLint("NewApi")
public class GoogleFitActivitySource extends ActivitySource {
    static final String SERVER_CLIENT_ID_METADATA_KEY = "com.fjuul.sdk.googlefit.server_client_id";
    static final String REQUEST_OFFLINE_ACCESS_METADATA_KEY = "com.fjuul.sdk.googlefit.request_offline_access";
    static final String GOOGLE_FIT_APP_PACKAGE_NAME = "com.google.android.apps.fitness";

    private static final ExecutorService sharedSequentialExecutor = createSequentialSingleCachedExecutor();

    private static volatile GoogleFitActivitySource instance;

    private final boolean requestOfflineAccess;
    private final @NonNull String serverClientId;
    private final @NonNull Set<FitnessMetricsType> collectableFitnessMetrics;
    private final @NonNull ActivitySourcesService sourcesService;
    private final @NonNull Context context;
    private final @NonNull GFDataManagerBuilder gfDataManagerBuilder;
    private final @NonNull ExecutorService localSequentialBackgroundExecutor;
    private volatile @Nullable Date lowerDateBoundary;

    static synchronized void initialize(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig sourcesManagerConfig) {
        final Context context = client.getAppContext();
        ApplicationInfo app;
        try {
            app =
                context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(
                "Can't retrieve config parameters for google-fit from the manifest: " + e.getLocalizedMessage());
        }
        final Bundle bundle = app.metaData;
        final boolean requestOfflineAccess = bundle.getBoolean(REQUEST_OFFLINE_ACCESS_METADATA_KEY, false);
        final String serverClientId = bundle.getString(SERVER_CLIENT_ID_METADATA_KEY);
        if (requestOfflineAccess && (serverClientId == null || serverClientId.isEmpty())) {
            throw new IllegalStateException("Can't retrieve meta-data for 'server_client_id' key from the manifest");
        }
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final GFDataUtils gfUtils = new GFDataUtils();
        final GFSyncMetadataStore syncMetadataStore = new GFSyncMetadataStore(client.getStorage());
        final GFDataManagerBuilder gfDataManagerBuilder =
            new GFDataManagerBuilder(context, gfUtils, syncMetadataStore, sourcesService);
        instance = new GoogleFitActivitySource(requestOfflineAccess,
            serverClientId,
            sourcesManagerConfig.getCollectableFitnessMetrics(),
            sourcesService,
            context,
            gfDataManagerBuilder,
            sharedSequentialExecutor);
    }

    /**
     * Return the initialized and configured instance of GoogleFitActivitySource. Make sure that this method is invoked
     * after ActivitySourcesManager.initialize. Otherwise, it throws IllegalStateException.
     *
     * @throws IllegalStateException if not initialized yet
     * @return instance of GoogleFitActivitySource
     */
    @NonNull
    public static GoogleFitActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "You must initialize ActivitySourceManager first before use GoogleFitActivitySource");
        }
        return instance;
    }

    GoogleFitActivitySource(boolean requestOfflineAccess,
        @NonNull String serverClientId,
        @NonNull Set<FitnessMetricsType> collectableFitnessMetrics,
        @NonNull ActivitySourcesService sourcesService,
        @NonNull Context context,
        @NonNull GFDataManagerBuilder gfDataManagerBuilder,
        @NonNull ExecutorService localSequentialBackgroundExecutor) {
        this.requestOfflineAccess = requestOfflineAccess;
        this.serverClientId = serverClientId;
        this.collectableFitnessMetrics = collectableFitnessMetrics;
        this.sourcesService = sourcesService;
        this.context = context;
        this.gfDataManagerBuilder = gfDataManagerBuilder;
        this.localSequentialBackgroundExecutor = localSequentialBackgroundExecutor;
    }

    /**
     * Checks whether all Google OAuth permissions are granted by the user to work with Google Fit. The list of all
     * needed permissions is determined by the set of the collectable fitness metrics. <br>
     * Note: an active current tracker connection to Google FIt does not always guarantee that the user grants all
     * permissions at the moment (for example, the user may have revoked them since the previous app session). To
     * request the needed permissions again, please use
     * {@link GoogleFitActivitySource#buildIntentRequestingFitnessPermissions}.
     *
     * @return boolean result of the check
     */
    public boolean areFitnessPermissionsGranted() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return areFitnessPermissionsGranted(account);
    }

    /**
     * Checks whether the Google Fit app is installed on the user's device. You don't have to have the installed
     * application to work with the Google Fit API. However, this is the easiest way to record and detect the user's
     * physical activity during the day.
     *
     * @return boolean result of the check
     */
    public boolean isGoogleFitAppInstalled() {
        return isGoogleFitAppInstalled(context);
    }

    /**
     * Checks whether the android.permission.ACTIVITY_RECOGNITION permission is given to your app. Starting with Android
     * 10 (API level 29), this permission is required to read session data. <br>
     * The SDK does not provide functionality for requesting this permission and you should follow the official <a href=
     * "https://developers.google.com/fit/android/authorization#requesting_android_permissions">documentation</a>.<br>
     * Note: you must have the declared permission {@code android.permission.ACTIVITY_RECOGNITION} in AndroidManifest
     * regardless of the target API. Otherwise, this method will return false.
     *
     * @return boolean result of the check
     */
    public boolean isActivityRecognitionPermissionGranted() {
        return isActivityRecognitionPermissionGranted(context);
    }

    /**
     * Build the intent that requests Google OAuth permissions according to the specified collectable fitness metrics.
     * You need to pass this intent to the #startActivityForResult method of Activity or Fragment in order to show the
     * prompting window. <br>
     * You don't need to call the {@link GoogleFitActivitySource#handleGoogleSignInResult} method in the method body of
     * #onActivityResult as in the case of the tracker connection. After that, the common permission status of the
     * collectable fitness metrics can be retrieved by using the
     * {@link GoogleFitActivitySource#areFitnessPermissionsGranted()} method.
     *
     * @return intent requesting permissions
     */
    @NonNull
    public Intent buildIntentRequestingFitnessPermissions() {
        final GoogleSignInClient signInClient = GoogleSignIn.getClient(context,
            buildGoogleSignInOptions(requestOfflineAccess, serverClientId, collectableFitnessMetrics));
        return signInClient.getSignInIntent();
    }

    /**
     * It verifies the status of the user's grant and a list of permissions granted by the user and finally completes
     * the connection to the GoogleFit tracker making a network request to the server.<br>
     * A result of this call is available in the callback which will be executed in the main thread. Dedicated error
     * results:
     * <ul>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException}.</li>
     * </ul>
     *
     * @param intent intent from the #onActivityResult method
     * @param callback callback for the result
     */
    public void handleGoogleSignInResult(@NonNull Intent intent, @NonNull Callback<Void> callback) {
        try {
            final Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            final GoogleSignInAccount account = task.getResult(ApiException.class);
            final boolean permissionsGranted = areFitnessPermissionsGranted(account);
            if (!permissionsGranted) {
                final Result<Void> result = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(result);
                return;
            }
            final Map<String, String> queryParams = new HashMap<>();
            final String authCode = account.getServerAuthCode();
            if (isOfflineAccessRequired() && authCode == null) {
                final Result<Void> result =
                    Result.error(new CommonException("No server auth code for the requested offline access"));
                callback.onResult(result);
                return;
            } else if (isOfflineAccessRequired() && authCode != null) {
                queryParams.put("code", authCode);
            }
            sourcesService.connect(getTrackerValue().getValue(), queryParams).enqueue((call, result) -> {
                if (result.isError()) {
                    callback.onResult(Result.error(result.getError()));
                    return;
                }
                final ConnectionResult connectionResult = result.getValue();
                // NOTE: android-sdk shouldn't support an external connection to google-fit
                if (connectionResult instanceof ConnectionResult.Connected) {
                    final Result<Void> success = Result.value(null);
                    callback.onResult(success);
                } else {
                    final FjuulException exception =
                        new FjuulException("Something wrong with the google fit connection: still not established");
                    final Result<Void> error = Result.error(exception);
                    callback.onResult(error);
                }
            });
        } catch (ApiException exc) {
            final Result<Void> error = Result.error(new CommonException("ApiException: " + exc.getMessage()));
            callback.onResult(error);
        }
    }

    /**
     * Returns a boolean indicating whether the offline access should be requested from a user. The offline access is
     * used for the GoogleFit connection supported by the server.
     *
     * @return boolean
     */
    public boolean isOfflineAccessRequired() {
        return requestOfflineAccess;
    }

    /**
     * Puts the task of synchronizing intraday data in a sequential execution queue (i.e., only one sync task can be
     * executed at a time) and will execute it when it comes to its turn. The synchronization result is available in the
     * callback.<br>
     * The task is atomic, so it will either succeed for all the specified types of metrics, or it will not succeed at
     * all.<br>
     * Note: date range of the synchronization is adjusted not only by the input options but the creation date of the
     * connection to Google Fit. So, any input dates that point to a date before the connection will be shifted to
     * conform to the allowed range.<br>
     * Dedicated result errors:
     * <ul>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException}.</li>
     * </ul>
     *
     * @param options intraday sync options
     * @param callback callback for the result
     */
    @SuppressLint("NewApi")
    public void syncIntradayMetrics(@NonNull final GoogleFitIntradaySyncOptions options,
        @Nullable final Callback<Void> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!areFitnessPermissionsGranted(account, options.getMetrics())) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        final GFDataManager GFDataManager = gfDataManagerBuilder.build(account, lowerDateBoundary);
        performTaskAlongWithCallback(() -> GFDataManager.syncIntradayMetrics(options), callback);
    }

    /**
     * Puts the task of synchronizing sessions in a sequential execution queue (i.e., only one sync task can be executed
     * at a time) and will execute it when it comes to its turn. The synchronization result is available in the
     * callback.<br>
     * Note: date range of the synchronization is adjusted not only by the input options but the creation date of the
     * connection to Google Fit. So, any input dates that point to a date before the connection will be shifted to
     * conform to the allowed range.<br>
     * Dedicated result errors:
     * <ul>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.ActivityRecognitionPermissionNotGrantedException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException}.</li>
     * </ul>
     *
     * @param options session sync options
     * @param callback callback for the result
     */
    public void syncSessions(@NonNull final GoogleFitSessionSyncOptions options,
        @Nullable final Callback<Void> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!areFitnessPermissionsGranted(account,
            Stream.of(FitnessMetricsType.WORKOUTS).collect(Collectors.toSet()))) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        if (!isActivityRecognitionPermissionGranted()) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(new ActivityRecognitionPermissionNotGrantedException(
                    "ACTIVITY_RECOGNITION permission not granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        final GFDataManager GFDataManager = gfDataManagerBuilder.build(account, lowerDateBoundary);
        performTaskAlongWithCallback(() -> GFDataManager.syncSessions(options), callback);
    }

    /**
     * Puts the task of synchronizing the user profile from Google Fit in a sequential execution queue (i.e., only one
     * sync task can be executed at a time) and will execute it when it comes to its turn. The synchronization result is
     * available in the callback.<br>
     * The task is atomic, so it will either succeed for all the specified types of metrics, or it will not succeed at
     * all.<br>
     * It's recommended to call this method before any other syncing methods of {@link GoogleFitActivitySource} because
     * the current profile state can affect the statistics calculation.<br>
     * The boolean result which is available in callback indicates if one of the specified metrics has been updated and
     * you need to refresh the user profile.<br>
     * Dedicated result errors:
     * <ul>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException};</li>
     * <li>{@link com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException}.</li>
     * </ul>
     *
     * @param options profile sync options
     * @param callback callback for the result
     */
    public void syncProfile(@NonNull final GoogleFitProfileSyncOptions options,
        @Nullable final Callback<Boolean> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!areFitnessPermissionsGranted(account, options.getMetrics())) {
            if (callback != null) {
                Result<Boolean> errorResult = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        final GFDataManager GFDataManager = gfDataManagerBuilder.build(account, lowerDateBoundary);
        performTaskAlongWithCallback(() -> GFDataManager.syncProfile(options), callback);
    }

    /**
     * Check whether the Google Fit app is installed on the user's device. You don't have to have the installed
     * application to work with the Google Fit API. However, this is the easiest way to record and detect the user's
     * physical activity during the day.
     *
     * @param appContext application context
     * @return boolean result of the check
     */
    public static boolean isGoogleFitAppInstalled(@NonNull Context appContext) {
        PackageManager packageManager = appContext.getPackageManager();
        try {
            packageManager.getPackageInfo(GOOGLE_FIT_APP_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether the android.permission.ACTIVITY_RECOGNITION permission is given to your app. Starting with Android
     * 10 (API level 29), this permission is required to read session data. <br>
     * The SDK does not provide functionality for requesting this permission and you should follow the official <a href=
     * "https://developers.google.com/fit/android/authorization#requesting_android_permissions">documentation</a>.<br>
     * Note: you must have the declared permission {@code android.permission.ACTIVITY_RECOGNITION} in AndroidManifest
     * regardless of the target API. Otherwise, this method will return false.
     *
     * @param context application context
     * @return boolean result of the check
     */
    public static boolean isActivityRecognitionPermissionGranted(@NonNull Context context) {
        boolean activityRecognitionListedInManifest;
        try {
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo packageInfo =
                packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            activityRecognitionListedInManifest =
                Optional.ofNullable(packageInfo.requestedPermissions).map((permissions) -> {
                    return Arrays.stream(permissions).anyMatch(Manifest.permission.ACTIVITY_RECOGNITION::equals);
                }).orElse(false);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
        // NOTE: dangerous permissions (aka runtime permissions) appeared since android api level 23
        // (Android 6.0 Marshmallow). Before that, they granted automatically.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return activityRecognitionListedInManifest;
        }
        final int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION);
        return activityRecognitionListedInManifest && result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    @NonNull
    protected TrackerValue getTrackerValue() {
        return TrackerValue.GOOGLE_FIT;
    }

    @NonNull
    Task<Void> disable() {
        // NOTE: for some weird reasons, ConfigClient#disableFit is not enough to revoke all granted OAuth permissions.
        // For that we use GoogleSignInClient#revokeAccess.
        // The GoogleSignInClient#revokeAccess task may be completed with error code 4 which is SIGN_IN_REQUIRED if a
        // user is not signed in yet. We don't propagate the error and then resolve it successfully.
        // The solution based on https://github.com/android/fit-samples/issues/28#issue-491885148

        // TODO: always revoke the full list of permissions ?
        final GoogleSignInOptions signInOptions =
            buildGoogleSignInOptions(requestOfflineAccess, serverClientId, collectableFitnessMetrics);
        return GoogleSignIn.getClient(context, signInOptions)
            .revokeAccess()
            .continueWithTask((revokeAccessTask) -> Tasks.forResult(null));
    }

    void setLowerDateBoundary(@Nullable Date lowerDateBoundary) {
        this.lowerDateBoundary = lowerDateBoundary;
    }

    private boolean areFitnessPermissionsGranted(@Nullable GoogleSignInAccount account) {
        return areFitnessPermissionsGranted(account, collectableFitnessMetrics);
    }

    private boolean areFitnessPermissionsGranted(@Nullable GoogleSignInAccount account,
        @NonNull Set<FitnessMetricsType> fitnessMetrics) {
        return GoogleFitActivitySource
            .areFitnessPermissionsGranted(account, requestOfflineAccess, serverClientId, fitnessMetrics);
    }

    private <T> void performTaskAlongWithCallback(@NonNull Supplier<Task<T>> taskSupplier,
        @Nullable Callback<T> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                T taskResult = Tasks.await(taskSupplier.get());
                Result<T> result = Result.value(taskResult);
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (ExecutionException | InterruptedException exc) {
                if (callback == null) {
                    return;
                }
                Throwable throwableToPropagate = exc;
                if (exc instanceof ExecutionException && exc.getCause() != null) {
                    throwableToPropagate = exc.getCause();
                }
                Result<T> errorResult = Result.error(throwableToPropagate);
                callback.onResult(errorResult);
            }
        });
    }

    @NonNull
    static GoogleSignInOptions buildGoogleSignInOptions(boolean offlineAccess,
        @Nullable String serverClientId,
        @NonNull Set<FitnessMetricsType> fitnessMetrics) {
        final FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();
        if (fitnessMetrics.contains(FitnessMetricsType.INTRADAY_CALORIES)) {
            fitnessOptionsBuilder.addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ);
        }
        if (fitnessMetrics.contains(FitnessMetricsType.INTRADAY_STEPS)) {
            fitnessOptionsBuilder.addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ);
        }
        if (fitnessMetrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE)) {
            fitnessOptionsBuilder.addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_READ);
        }
        if (fitnessMetrics.contains(FitnessMetricsType.WORKOUTS)) {
            fitnessOptionsBuilder.accessActivitySessions(FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_POWER_SAMPLE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ);
        }
        if (fitnessMetrics.contains(FitnessMetricsType.HEIGHT)) {
            fitnessOptionsBuilder.addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ);
        }
        if (fitnessMetrics.contains(FitnessMetricsType.WEIGHT)) {
            fitnessOptionsBuilder.addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ);
        }
        final FitnessOptions fitnessOptions = fitnessOptionsBuilder.build();
        GoogleSignInOptions.Builder googleSignInOptionsBuilder = new GoogleSignInOptions.Builder();
        googleSignInOptionsBuilder.addExtension(fitnessOptions);
        if (offlineAccess) {
            // TODO: determine exactly scopes required by the server
            // see
            // https://github.com/fjuul/commons-js/blob/d067a458c4b1eb88497c5c4c5b961d4891f7f08e/src/services/trackers/googleFit/GoogleFitClient.ts#L49-L54
            // NOTE: currently the server uses all scopes because it tries to sync every possible fitness data of a
            // user.
            // Since we are not sure about the future of the GF backend integration, scopes listed as-is (see the link
            // above).
            // Ideally, the server implementation of the syncing should safely request data only for the granted scopes.
            final Scope[] hrScope = FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
                .build()
                .getImpliedScopes()
                .toArray(new Scope[0]);
            googleSignInOptionsBuilder = googleSignInOptionsBuilder.requestProfile()
                .requestScopes(Fitness.SCOPE_ACTIVITY_READ, Fitness.SCOPE_BODY_READ)
                .requestScopes(Fitness.SCOPE_LOCATION_READ, hrScope)
                .requestServerAuthCode(serverClientId, true);
        }
        return googleSignInOptionsBuilder.build();
    }

    static boolean areFitnessPermissionsGranted(@Nullable GoogleSignInAccount account,
        boolean offlineAccess,
        String serverClientId,
        @NonNull Set<FitnessMetricsType> fitnessMetrics) {
        if (account == null) {
            return false;
        }
        final Set<Scope> grantedScopes = account.getGrantedScopes();
        return grantedScopes
            .containsAll(buildGoogleSignInOptions(offlineAccess, serverClientId, fitnessMetrics).getScopes());
    }

    private static ExecutorService createSequentialSingleCachedExecutor() {
        // NOTE: this solution works only for single thread (do not edit maximumPoolSize)
        return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }
}

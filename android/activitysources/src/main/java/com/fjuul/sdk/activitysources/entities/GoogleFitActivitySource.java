package com.fjuul.sdk.activitysources.entities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.ActivityRecognitionPermissionNotGrantedException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.FjuulException;
import com.fjuul.sdk.core.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.ConfigClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SuppressLint("NewApi")
public class GoogleFitActivitySource extends ActivitySource {
    static final String SERVER_CLIENT_ID_METADATA_KEY = "com.fjuul.sdk.googlefit.server_client_id";
    static final String REQUEST_OFFLINE_ACCESS_METADATA_KEY = "com.fjuul.sdk.googlefit.request_offline_access";
    static final String GOOGLE_FIT_APP_PACKAGE_NAME = "com.google.android.apps.fitness";

    private static final ExecutorService localSequentialBackgroundExecutor = createSequentialSingleCachedExecutor();

    private static volatile GoogleFitActivitySource instance;

    private final boolean requestOfflineAccess;
    private final @NonNull String serverClientId;
    private final @NonNull ActivitySourcesService sourcesService;
    private final @NonNull GFDataUtils gfDataUtils;
    private final @NonNull GFSyncMetadataStore syncMetadataStore;
    private final @NonNull Context context;

    static synchronized void initialize(@NonNull ApiClient client) {
        Context context = client.getAppContext();
        ApplicationInfo app;
        try {
            app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Can't retrieve config parameters for google-fit from the manifest: " + e.getLocalizedMessage());
        }
        Bundle bundle = app.metaData;
        boolean requestOfflineAccess = bundle.getBoolean(REQUEST_OFFLINE_ACCESS_METADATA_KEY, false);
        String serverClientId = bundle.getString(SERVER_CLIENT_ID_METADATA_KEY);
        if (serverClientId == null || serverClientId.isEmpty()) {
            throw new IllegalStateException("Can't retrieve meta-data for 'server_client_id' key from the manifest");
        }
        ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        GFDataUtils gfUtils = new GFDataUtils();
        GFSyncMetadataStore syncMetadataStore = new GFSyncMetadataStore(client.getStorage(), client.getUserToken());
        instance = new GoogleFitActivitySource(requestOfflineAccess, serverClientId, sourcesService, gfUtils, syncMetadataStore, context);
    }

    public static GoogleFitActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException("You must initialize ActivitySourceManager first before use GoogleFitActivitySource");
        }
        return instance;
    }

    GoogleFitActivitySource(boolean requestOfflineAccess,
                            @NonNull String serverClientId,
                            @NonNull ActivitySourcesService sourcesService,
                            @NonNull GFDataUtils gfDataUtils,
                            @NonNull GFSyncMetadataStore syncMetadataStore,
                            @NonNull Context context) {
        this.requestOfflineAccess = requestOfflineAccess;
        this.serverClientId = serverClientId;
        this.sourcesService = sourcesService;
        this.gfDataUtils = gfDataUtils;
        this.syncMetadataStore = syncMetadataStore;
        this.context = context;
    }

    public boolean areFitnessPermissionsGranted() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return areFitnessPermissionsGranted(account);
    }

    public boolean isGoogleFitAppInstalled() {
        return isGoogleFitAppInstalled(context);
    }

    // TODO: javadoc
    public boolean isActivityRecognitionPermissionGranted() {
        return isActivityRecognitionPermissionGranted(context);
    }

    Task<Void> disable() {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return Tasks.forResult(null);
        }
        final ConfigClient configClient = Fitness.getConfigClient(context, account);
        // NOTE: for some weird reasons, ConfigClient#disableFit is not enough to revoke all
        // granted OAuth permissions. Then we revoke these explicitly via GoogleSignInClient#revokeAccess.
        // However then revokeAccess task resolves with error code 4 which is SIGN_IN_REQUIRED.
        // The solution based on https://github.com/android/fit-samples/issues/28#issuecomment-557865949
        final Task<Void> disableFitAndRevokeAccessTask = configClient.disableFit()
            .continueWithTask((disableFitTask) -> {
                final GoogleSignInOptions signInOptions = buildGoogleSignInOptions(requestOfflineAccess, serverClientId);
                return GoogleSignIn.getClient(context, signInOptions).revokeAccess()
                    .continueWithTask((revokeAccessTask) -> Tasks.forResult(null));
            });
        return disableFitAndRevokeAccessTask;
    }

    public void handleGoogleSignInResult(@NonNull Intent intent, @NonNull Callback<Void> callback) {
        try {
            final Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            final GoogleSignInAccount account = task.getResult(ApiException.class);
            final boolean permissionsGranted = areFitnessPermissionsGranted(account);
            if (!permissionsGranted) {
                Result<Void> result = Result.error(new CommonException("Not all permissions were granted"));
                callback.onResult(result);
                return;
            }
            final Map<String, String> queryParams = new HashMap<>();
            final String authCode = account.getServerAuthCode();
            if (isOfflineAccessRequired() && authCode == null) {
                Result<Void> result = Result.error(new CommonException("No server auth code for the requested offline access"));
                callback.onResult(result);
                return;
            } else if (isOfflineAccessRequired() && authCode != null) {
                queryParams.put("code", authCode);
            }
            sourcesService.connect(getTrackerValue().getValue(), queryParams).enqueue((call, result) -> {
                if (result.isError()) {
                    callback.onResult(Result.error(result.getError()));
                }
                ConnectionResult connectionResult = result.getValue();
                // NOTE: android-sdk shouldn't support an external connection to google-fit
                if (connectionResult instanceof ConnectionResult.Connected) {
                    Result<Void> success = Result.value(null);
                    callback.onResult(success);
                } else {
                    FjuulException exception = new FjuulException("Something wrong with the google fit connection: still not established");
                    Result<Void> error = Result.error(exception);
                    callback.onResult(error);
                }
            });
        } catch (ApiException exc) {
            Result<Void> error = Result.error(new CommonException("ApiException: " + exc.getMessage()));
            callback.onResult(error);
        }
    }

    public boolean isOfflineAccessRequired() {
        return requestOfflineAccess;
    }

    // TODO: javadoc (the expected error is FitnessPermissionsNotGrantedException)
    @SuppressLint("NewApi")
    public void syncIntradayMetrics(@NonNull final GFIntradaySyncOptions options, @Nullable final Callback<Void> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!areFitnessPermissionsGranted(account)) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        final GoogleFitDataManager googleFitDataManager = prepareGoogleFitDataManager(account);
        performTaskAlongWithCallback(() -> googleFitDataManager.syncIntradayMetrics(options), callback);
    }

    // TODO: javadoc (the expected error is FitnessPermissionsNotGrantedException, ActivityRecognitionPermissionNotGrantedException)
    public void syncSessions(@NonNull final GFSessionSyncOptions options, @Nullable final Callback<Void> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!areFitnessPermissionsGranted(account)) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(
                    new FitnessPermissionsNotGrantedException("Not all required GoogleFit permissions were granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        if (!isActivityRecognitionPermissionGranted()) {
            if (callback != null) {
                Result<Void> errorResult = Result.error(
                    new ActivityRecognitionPermissionNotGrantedException("ACTIVITY_RECOGNITION permission not granted"));
                callback.onResult(errorResult);
            }
            return;
        }
        final GoogleFitDataManager googleFitDataManager = prepareGoogleFitDataManager(account);
        performTaskAlongWithCallback(() -> googleFitDataManager.syncSessions(options), callback);
    }

    public static boolean isGoogleFitAppInstalled(@NonNull Context appContext) {
        PackageManager packageManager = appContext.getPackageManager();
        try {
            packageManager.getPackageInfo(GOOGLE_FIT_APP_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // TODO: javadoc
    public static boolean isActivityRecognitionPermissionGranted(@NonNull Context context) {
        // NOTE: dangerous permissions (aka runtime permissions) appeared since android api level 23
        // (Android 6.0 Marshmallow). Before that, they granted automatically.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    protected Intent buildIntentRequestingPermissions() {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(context, buildGoogleSignInOptions(requestOfflineAccess, serverClientId));
        return signInClient.getSignInIntent();
    }

    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.GOOGLE_FIT;
    }

    private boolean areFitnessPermissionsGranted(@Nullable GoogleSignInAccount account) {
        return GoogleFitActivitySource.areFitnessPermissionsGranted(account, requestOfflineAccess, serverClientId);
    }

    private GoogleFitDataManager prepareGoogleFitDataManager(@NonNull GoogleSignInAccount account) {
        final HistoryClient historyClient = Fitness.getHistoryClient(context, account);
        final SessionsClient sessionsClient = Fitness.getSessionsClient(context, account);
        final GFClientWrapper clientWrapper = new GFClientWrapper(historyClient, sessionsClient, gfDataUtils);
        return new GoogleFitDataManager(clientWrapper, gfDataUtils, syncMetadataStore, sourcesService);
    }

    private <T> void performTaskAlongWithCallback(@NonNull Supplier<Task<T>> taskSupplier, @Nullable Callback<T> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                T taskResult = Tasks.await(taskSupplier.get());
                Result<T> result = Result.value(taskResult);
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (ExecutionException | InterruptedException exc) {
                Result<T> errorResult = Result.error(exc);
                if (callback != null) {
                    callback.onResult(errorResult);
                }
            }
        });
    }

    private static GoogleSignInOptions buildGoogleSignInOptions(boolean offlineAccess, String serverClientId) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                new Scope(Scopes.FITNESS_LOCATION_READ),
                new Scope(Scopes.FITNESS_BODY_READ));
        // TODO: use fitness options to explicitly specify what data types will be used for the google fit data synchronization
        FitnessOptions fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build();
        builder.addExtension(fitnessOptions);
        if (offlineAccess) {
            builder = builder.requestProfile().requestServerAuthCode(serverClientId, true);
        }
        return builder.build();
    }

    static boolean areFitnessPermissionsGranted(@Nullable GoogleSignInAccount account, boolean offlineAccess, String serverClientId) {
        if (account == null) {
            return false;
        }
        Set<Scope> grantedScopes = account.getGrantedScopes();
        return grantedScopes.containsAll(buildGoogleSignInOptions(offlineAccess, serverClientId).getScopes());
    }

    private static ExecutorService createSequentialSingleCachedExecutor() {
        // NOTE: this solution works only for single thread (do not edit maximumPoolSize)
        return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }
}

package com.fjuul.sdk.activitysources.entities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.NotGrantedPermissionsException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.entities.Callback;
import com.fjuul.sdk.entities.Result;
import com.fjuul.sdk.exceptions.FjuulException;
import com.fjuul.sdk.http.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.ConfigClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
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
public final class GoogleFitActivitySource extends ActivitySource {
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

    public GoogleFitActivitySource(boolean requestOfflineAccess,
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

    public boolean arePermissionsGranted() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return arePermissionsGranted(account);
    }

    public boolean isGoogleFitAppInstalled() {
        return isGoogleFitAppInstalled(context);
    }

    // TODO: javadoc
    public boolean isActivityRecognitionPermissionGranted() {
        return isActivityRecognitionPermissionGranted(context);
    }

    void disable(@Nullable Callback<Void> callback) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            if (callback != null) {
                callback.onResult(Result.value(null));
            }
            return;
        }
        final ConfigClient configClient = Fitness.getConfigClient(context, account);
        configClient.disableFit().addOnCompleteListener(task -> {
           if (task.isCanceled() || !task.isSuccessful()) {
               if (callback != null) {
                   callback.onResult(Result.error(task.getException()));
               }
               return;
           }
           if (callback != null) {
               callback.onResult(Result.value(null));
           }
        });
    }

    public void handleGoogleSignInResult(@NonNull Intent intent, @NonNull Callback<Void> callback) {
        try {
            final Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            final GoogleSignInAccount account = task.getResult(ApiException.class);
            final boolean permissionsGranted = arePermissionsGranted(account);
            if (!permissionsGranted) {
                Result<Void> result = Result.error(new CommonException("Not all permissions were granted"));
                callback.onResult(result);
                return;
            }
            if (!isOfflineAccessRequested()) {
                Result<Void> result = Result.value(null);
                callback.onResult(result);
            }
            String authCode = account.getServerAuthCode();
            if (authCode == null) {
                Result<Void> result = Result.error(new CommonException("No server auth code for the requested offline access"));
                callback.onResult(result);
            }
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("code", authCode);
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

    public boolean isOfflineAccessRequested() {
        return requestOfflineAccess;
    }

    @SuppressLint("NewApi")
    public void syncIntradayMetrics(@NonNull final GFIntradaySyncOptions options, @Nullable final Callback<Void> callback) {
        GoogleFitDataManager tempGoogleFitDataManager;
        try {
            tempGoogleFitDataManager = prepareGoogleFitDataManager();
        } catch (NotGrantedPermissionsException exc) {
            Result<Void> errorResult = Result.error(exc);
            if (callback != null) {
                callback.onResult(errorResult);
            }
            return;
        }
        final GoogleFitDataManager googleFitDataManager = tempGoogleFitDataManager;
        performTaskAlongWithCallback(() -> googleFitDataManager.syncIntradayMetrics(options), callback);
    }

    public void syncSessions(@NonNull final GFSessionSyncOptions options, @Nullable final Callback<Void> callback) {
        GoogleFitDataManager tempGoogleFitDataManager;
        try {
            tempGoogleFitDataManager = prepareGoogleFitDataManager();
        } catch (NotGrantedPermissionsException exc) {
            Result<Void> errorResult = Result.error(exc);
            if (callback != null) {
                callback.onResult(errorResult);
            }
            return;
        }
        final GoogleFitDataManager googleFitDataManager = tempGoogleFitDataManager;
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

    private boolean arePermissionsGranted(@Nullable GoogleSignInAccount account) {
        return GoogleFitActivitySource.arePermissionsGranted(account, requestOfflineAccess, serverClientId);
    }

    private GoogleFitDataManager prepareGoogleFitDataManager() throws NotGrantedPermissionsException {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!arePermissionsGranted(account)) {
            throw new NotGrantedPermissionsException("Not all permissions were granted");
        }
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
                Result errorResult = Result.error(exc);
                if (callback != null) {
                    callback.onResult(errorResult);
                }
            }
        });
    }

    private static GoogleSignInOptions buildGoogleSignInOptions(boolean offlineAccess, String serverClientId) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ), new Scope(Scopes.FITNESS_LOCATION_READ), new Scope(Scopes.FITNESS_BODY_READ));
        if (offlineAccess) {
            builder = builder.requestProfile().requestServerAuthCode(serverClientId, true);
        }
        return builder.build();
    }

    static boolean arePermissionsGranted(@Nullable GoogleSignInAccount account, boolean offlineAccess, String serverClientId) {
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

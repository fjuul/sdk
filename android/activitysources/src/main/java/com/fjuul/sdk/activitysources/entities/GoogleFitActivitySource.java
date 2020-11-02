package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions.NotGrantedPermissionsException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.http.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;

import java.util.Set;

@SuppressLint("NewApi")
public final class GoogleFitActivitySource {
    static final String SERVER_CLIENT_ID_METADATA_KEY = "com.fjuul.sdk.googlefit.server_client_id";
    static final String REQUEST_OFFLINE_ACCESS_METADATA_KEY = "com.fjuul.sdk.googlefit.request_offline_access";

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

    public boolean arePermissionsGranted(@NonNull GoogleSignInAccount account) {
        return GoogleFitActivitySource.arePermissionsGranted(account, requestOfflineAccess, serverClientId);
    }

    public boolean isOfflineAccessRequested() {
        return requestOfflineAccess;
    }

    @SuppressLint("NewApi")
    public void syncIntradayMetrics(GFIntradaySyncOptions options) {
        GoogleFitDataManager googleFitDataManager = null;
        try {
            googleFitDataManager = prepareGoogleFitDataManager();
        } catch (NotGrantedPermissionsException exception) {
            Log.d("GOOGLE_FIT", "syncIntradayMetrics: " + exception);
            // pass it to callback
            // and return
            return;
        }
        googleFitDataManager.syncIntradayMetrics(options);
    }

    public void syncSessions(@NonNull Context context, GFSessionSyncOptions options) {
        GoogleFitDataManager googleFitDataManager = null;
        try {
            googleFitDataManager = prepareGoogleFitDataManager();
        } catch (NotGrantedPermissionsException exception) {
            Log.d("GOOGLE_FIT", "syncSessions: " + exception);
            // pass it to callback
            // and return
            return;
        }
        googleFitDataManager.syncSessions(options);
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

    // TODO: add static method checking if google fit is installed in the system

    protected Intent buildIntentRequestingPermissions(@NonNull Context context) {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(context, buildGoogleSignInOptions(requestOfflineAccess, serverClientId));
        return signInClient.getSignInIntent();
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

    // TODO: use general callback interface
    public interface HandleSignInResultCallback {
        void onResult(@Nullable Exception exception, boolean success);
    }
}

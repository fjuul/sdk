package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private boolean requestOfflineAccess;
    private @NonNull ApiClient client;
    private @NonNull String serverClientId;

    static synchronized void initialize(ApiClient client) {
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
        instance = new GoogleFitActivitySource(client, requestOfflineAccess, serverClientId);
    }

    public static GoogleFitActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException("You must initialize ActivitySourceManager first before use GoogleFitActivitySource");
        }
        return instance;
    }

    private GoogleFitActivitySource(ApiClient client, boolean requestOfflineAccess, String serverClientId) {
        this.client = client;
        this.requestOfflineAccess = requestOfflineAccess;
        this.serverClientId = serverClientId;
    }

    public boolean arePermissionsGranted(@NonNull Context context) {
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
    public void syncIntradayMetrics(@NonNull Context context, GFIntradaySyncOptions options) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!arePermissionsGranted(account)) {
            throw new IllegalStateException("Not all permissions were granted");
        }
        HistoryClient historyClient = Fitness.getHistoryClient(context, account);
        SessionsClient sessionsClient = Fitness.getSessionsClient(context, account);
        GFDataUtils gfUtils = new GFDataUtils();
        GFClientWrapper clientWrapper = new GFClientWrapper(historyClient, sessionsClient, gfUtils);
        GFSyncMetadataStore gfSyncMetadataStore = new GFSyncMetadataStore(client.getStorage(), client.getUserToken());
        GoogleFitDataManager gfDataManager = new GoogleFitDataManager(clientWrapper, gfUtils, gfSyncMetadataStore);
        gfDataManager.syncIntradayMetrics(options);
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

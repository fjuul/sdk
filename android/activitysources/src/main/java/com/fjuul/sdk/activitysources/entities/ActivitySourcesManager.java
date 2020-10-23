package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.entities.PersistentStorage;
import com.fjuul.sdk.http.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.tasks.Task;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActivitySourcesManager {
    private ActivitySourcesService sourcesService;
    private ActivitySourcesStateStore stateStore;
    @Nullable private List<TrackerConnection> currentConnections;

    @Nullable private static ActivitySourcesManager instance;

    ActivitySourcesManager(ActivitySourcesService sourcesService, ActivitySourcesStateStore stateStore, List<TrackerConnection> connections) {
        this.sourcesService = sourcesService;
        this.stateStore = stateStore;
        this.currentConnections = connections;
    }

    @SuppressLint("NewApi")
    public static void initialize(ApiClient client) {
        // (get the configured api-client from context ?)
        ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage(), client.getUserToken());
        List<TrackerConnection> connections = stateStore.getConnections().orElse(null);
        ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        instance = new ActivitySourcesManager(sourcesService, stateStore, connections);
        // TODO: inject api-client to GF activity source
    }

    // TODO: add overloaded methods for all external trackers

    public Intent connect(@NonNull GoogleFitActivitySource gfActivitySource, @NonNull Context context) {
        return gfActivitySource.buildIntentRequestingPermissions(context);
    }

    public void handleGoogleSignInResult(@NonNull GoogleFitActivitySource gfActivitySource, @NonNull Intent intent, @NonNull GoogleFitActivitySource.HandleSignInResultCallback callback) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            boolean permissionsGranted = gfActivitySource.arePermissionsGranted(account);
            if (!permissionsGranted) {
                callback.onResult(new CommonException("Not all permissions were granted"), false);
                return;
            }
            if (!gfActivitySource.isOfflineAccessRequested()) {
                callback.onResult(null, true);
            }
            String authCode = account.getServerAuthCode();
            if (authCode == null) {
                callback.onResult(new CommonException("No server auth code for the requested offline access"), false);
            }
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("code", authCode);
            sourcesService.connect("googlefit", queryParams).enqueue((call, result) -> {
                if (result.isError()) {
                    callback.onResult(result.getError(), false);
                }
                ConnectionResult connectionResult = result.getValue();
                // NOTE: android-sdk shouldn't support an external connection to google-fit
                if (connectionResult instanceof ConnectionResult.Connected) {
                    callback.onResult(null, true);
                }
            });
        } catch (ApiException exc) {
            callback.onResult(new CommonException("ApiException: " + exc.getMessage()), false);
        }
    }

    public GoogleFitDataManager createGoogleFitDataManager(@NonNull Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        // NOTE: we set offlineAccess to false here because GoogleFitDataManager works only with local fitness data
        if (GoogleFitActivitySource.arePermissionsGranted(account, false)) {
            HistoryClient historyClient = Fitness.getHistoryClient(context, account);
            SessionsClient sessionsClient = Fitness.getSessionsClient(context, account);
            GFDataUtils gfUtils = new GFDataUtils();
            GFClientWrapper clientWrapper = new GFClientWrapper(historyClient, sessionsClient, gfUtils);
            String userToken = sourcesService.getUserToken();
            GFSyncMetadataStore gfSyncMetadataStore = new GFSyncMetadataStore(new PersistentStorage(context), userToken);
            return new GoogleFitDataManager(clientWrapper, gfUtils, gfSyncMetadataStore);
        } else {
            throw new IllegalStateException("Not all permissions were granted");
        }
    }
}

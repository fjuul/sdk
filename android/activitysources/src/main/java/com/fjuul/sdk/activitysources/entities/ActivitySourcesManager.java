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
import com.google.android.gms.tasks.Task;

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
    public static synchronized void initialize(ApiClient client) {
        // (get the configured api-client from context ?)
        ActivitySourcesStateStore stateStore = new ActivitySourcesStateStore(client.getStorage(), client.getUserToken());
        List<TrackerConnection> connections = stateStore.getConnections().orElse(null);
        ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        GoogleFitActivitySource.initialize(client);
        instance = new ActivitySourcesManager(sourcesService, stateStore, connections);
    }

    @NonNull
    public static ActivitySourcesManager getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("You must initialize first before getting the instance");
        }
        return instance;
    }

    // TODO: add a single unified connect method which works with activity source polymorphically.

    public Intent connect(@NonNull GoogleFitActivitySource gfActivitySource, @NonNull Context context) {
        return gfActivitySource.buildIntentRequestingPermissions(context);
    }

    // TODO: use the unified callback interface
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
}

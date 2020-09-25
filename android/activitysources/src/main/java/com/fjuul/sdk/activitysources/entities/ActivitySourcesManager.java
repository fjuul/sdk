package com.fjuul.sdk.activitysources.entities;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.entities.PersistentStorage;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.tasks.Task;

public final class ActivitySourcesManager {
    private ActivitySourcesService sourcesService;

    public ActivitySourcesManager(ActivitySourcesService sourcesService) {
        this.sourcesService = sourcesService;
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
                callback.onResult(new Error("Not all permissions were granted"), false);
                return;
            }
            if (!gfActivitySource.isOfflineAccessRequested()) {
                callback.onResult(null, true);
            }
            String authCode = account.getServerAuthCode();
            if (authCode == null) {
                callback.onResult(new Error("No server auth code for the requested offline access"), false);
            }
            // TODO: use sourcesService to send auth code to the back-end
            callback.onResult(null, false);
        } catch (ApiException exc) {
            callback.onResult(new Error("ApiException: " + exc.getMessage()), false);
        }
    }

    public GoogleFitDataManager createGoogleFitDataManager(@NonNull Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        // NOTE: we set offlineAccess to false here because GoogleFitDataManager works only with local fitness data
        if (GoogleFitActivitySource.arePermissionsGranted(account, false)) {
            HistoryClient client = Fitness.getHistoryClient(context, account);
            GFHistoryClientWrapper clientWrapper = new GFHistoryClientWrapper(client);
            String userToken = sourcesService.getUserToken();
            GFSyncMetadataStore gfSyncMetadataStore = new GFSyncMetadataStore(new PersistentStorage(context), userToken);
            return new GoogleFitDataManager(clientWrapper, new GFDataUtils(), gfSyncMetadataStore);
        } else {
            throw new IllegalStateException("Not all permissions were granted");
        }
    }
}

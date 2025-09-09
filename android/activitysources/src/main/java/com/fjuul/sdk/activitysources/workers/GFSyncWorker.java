package com.fjuul.sdk.activitysources.workers;

import java.util.Collections;
import java.util.List;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.UserCredentials;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public abstract class GFSyncWorker extends Worker {
    public static final String KEY_USER_TOKEN_ARG = "USER_TOKEN";
    public static final String KEY_USER_SECRET_ARG = "USER_SECRET";
    public static final String KEY_API_KEY_ARG = "API_KEY";
    public static final String KEY_BASE_URL_ARG = "BASE_URL";

    public GFSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    protected ActivitySourcesManager getOrInitializeActivitySourcesManager() {
        ActivitySourcesManager sourcesManager;
        try {
            sourcesManager = ActivitySourcesManager.getInstance();
        } catch (IllegalStateException exception) {
            // TODO: construct the isolated instance of ActivitySourcesManager to avoid the situation when a resurrected
            // application has the wrong state after background workers?
            // ActivitySourcesManager is not initialized yet
            final String userToken = getInputData().getString(KEY_USER_TOKEN_ARG);
            final String userSecret = getInputData().getString(KEY_USER_SECRET_ARG);
            final String apiKey = getInputData().getString(KEY_API_KEY_ARG);
            final String baseUrl = getInputData().getString(KEY_BASE_URL_ARG);
            final ApiClient client = new ApiClient.Builder(getApplicationContext(), baseUrl, apiKey)
                .setUserCredentials(new UserCredentials(userToken, userSecret))
                .build();
            // NOTE: here we build the config with the untouched mode because we don't want to reset
            // previously scheduled periodic gf sync works.
            // NOTE: the empty set is a workaround to initialize the sources manager from background workers.
            // It would be good to create an internal non-static instance of ActivitySourcesManager for background
            // workers with their own isolated state.
            final ActivitySourcesManagerConfig config =
                new ActivitySourcesManagerConfig.Builder().keepUntouchedBackgroundSync()
                    .setCollectableFitnessMetrics(Collections.emptySet())
                    .build();
            ActivitySourcesManager.initialize(client, config);
            sourcesManager = ActivitySourcesManager.getInstance();
        }
        return sourcesManager;
    }

    @SuppressLint("NewApi")
    @Nullable
    protected static ActivitySourceConnection getGoogleFitActivitySourceConnection(
        @NonNull ActivitySourcesManager manager) {
        final List<ActivitySourceConnection> currentConnections = manager.getCurrent();
        if (currentConnections == null) {
            return null;
        }
        return currentConnections.stream()
            .filter(connection -> connection.getActivitySource() instanceof GoogleFitActivitySource)
            .findFirst()
            .orElse(null);
    }
}

package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.ApiClient;

public abstract class GoogleFitSyncWorker extends Worker {
    public static final String KEY_USER_TOKEN_ARG = "USER_TOKEN";
    public static final String KEY_USER_SECRET_ARG = "USER_SECRET";
    public static final String KEY_API_KEY_ARG = "API_KEY";
    public static final String KEY_BASE_URL_ARG = "BASE_URL";

    public GoogleFitSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected ActivitySourcesManager getOrInitializeActivitySourcesManager() {
        ActivitySourcesManager sourcesManager;
        try {
            sourcesManager = ActivitySourcesManager.getInstance();
        } catch (IllegalStateException exception) {
            // TODO: construct the isolated instance of activity sources manager to avoid the case ?
            // ActivitySourcesManager is not initialized yet
            String userToken = getInputData().getString(KEY_USER_TOKEN_ARG);
            String userSecret = getInputData().getString(KEY_USER_SECRET_ARG);
            String apiKey = getInputData().getString(KEY_API_KEY_ARG);
            String baseUrl = getInputData().getString(KEY_BASE_URL_ARG);
            ApiClient client = new ApiClient.Builder(getApplicationContext(), baseUrl, apiKey)
                .setUserCredentials(new UserCredentials(userToken, userSecret))
                .build();
            // TODO: setup config
            ActivitySourcesManager.initialize(client, null);
            sourcesManager = ActivitySourcesManager.getInstance();
        }
        return sourcesManager;
    }

    @SuppressLint("NewApi")
    @Nullable
    protected static ActivitySourceConnection getGoogleFitActivitySourceConnection(ActivitySourcesManager manager) {
        return manager.getCurrent().stream()
            .filter(connection -> connection.getActivitySource() instanceof GoogleFitActivitySource)
            .findFirst()
            .orElse(null);
    }
}

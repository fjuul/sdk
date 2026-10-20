package com.fjuul.sdk.activitysources.workers;

import java.util.List;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;

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

    // Null when the active instance belongs to another session than the one that queued this work.
    @Nullable
    ActivitySourcesManager getSessionActivitySourcesManager() {
        return ActivitySourcesManager.getOrInitializeForWorker(getApplicationContext(), getInputData());
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

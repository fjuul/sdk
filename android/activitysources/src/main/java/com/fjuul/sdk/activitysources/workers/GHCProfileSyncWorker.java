package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectProfileSyncOptions;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.ExecutionException;

public class GHCProfileSyncWorker extends GHCSyncWorker {
    public static final String KEY_PROFILE_METRICS_ARG = "PROFILE_METRICS";

    public GHCProfileSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final ActivitySourcesManager sourcesManager = getOrInitializeActivitySourcesManager();
        final ActivitySourceConnection ghcConnection = getGoogleHealthConnectActivitySourceConnection(sourcesManager);
        if (ghcConnection == null) {
            // NOTE: currently, the task will be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        final GoogleHealthConnectActivitySource gfSource = ((GoogleHealthConnectActivitySource) ghcConnection.getActivitySource());
        final TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        final GoogleHealthConnectProfileSyncOptions syncOptions = buildProfileSyncOptions();
        gfSource.syncProfile(syncOptions, (result -> {
            if (result.isError() && result.getError() instanceof Exception) {
                taskCompletionSource.trySetException((Exception) result.getError());
                return;
            }
            taskCompletionSource.trySetResult(null);
        }));
        try {
            Tasks.await(taskCompletionSource.getTask());
            return Result.success();
        } catch (ExecutionException exception) {
            Exception originCause = (Exception) exception.getCause();
            // TODO: figure out what caused the exception and handle this
        } catch (InterruptedException e) {}
        return Result.failure();
    }

    @SuppressLint("NewApi")
    private GoogleHealthConnectProfileSyncOptions buildProfileSyncOptions() {
        final String[] rawProfileMetrics = getInputData().getStringArray(KEY_PROFILE_METRICS_ARG);
        final GoogleHealthConnectProfileSyncOptions.Builder syncOptionsBuilder = new GoogleHealthConnectProfileSyncOptions.Builder();
        for (final String rawProfileMetric : rawProfileMetrics) {
            try {
                final FitnessMetricsType metric = FitnessMetricsType.valueOf(rawProfileMetric);
                syncOptionsBuilder.include(metric);
            } catch (Exception e) {}
        }
        return syncOptionsBuilder.build();
    }
}

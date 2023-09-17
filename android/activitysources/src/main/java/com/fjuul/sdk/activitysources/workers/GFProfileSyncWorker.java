package com.fjuul.sdk.activitysources.workers;

import java.util.concurrent.ExecutionException;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

// NOTE: currently it inherits from GFSyncWorker and syncs only with Google Fit but in the future, it will be changed.
public class GFProfileSyncWorker extends GFSyncWorker {
    public static final String KEY_PROFILE_METRICS_ARG = "PROFILE_METRICS";

    public GFProfileSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final ActivitySourcesManager sourcesManager = getOrInitializeActivitySourcesManager();
        final ActivitySourceConnection gfConnection = getGoogleFitActivitySourceConnection(sourcesManager);
        if (gfConnection == null) {
            // NOTE: currently, the task will be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        final GoogleFitActivitySource gfSource = ((GoogleFitActivitySource) gfConnection.getActivitySource());
        final TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        final GoogleFitProfileSyncOptions syncOptions = buildProfileSyncOptions();
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
    private GoogleFitProfileSyncOptions buildProfileSyncOptions() {
        final String[] rawProfileMetrics = getInputData().getStringArray(KEY_PROFILE_METRICS_ARG);
        final GoogleFitProfileSyncOptions.Builder syncOptionsBuilder = new GoogleFitProfileSyncOptions.Builder();
        for (final String rawProfileMetric : rawProfileMetrics) {
            try {
                final FitnessMetricsType metric = FitnessMetricsType.valueOf(rawProfileMetric);
                if (metric != null) {
                    syncOptionsBuilder.include(metric);
                }
            } catch (Exception e) {}
        }
        return syncOptionsBuilder.build();
    }
}

package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectIntradaySyncOptions;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

public class GHCIntradaySyncWorker extends GHCSyncWorker {
    public static final String KEY_INTRADAY_METRICS_ARG = "INTRADAY_METRICS";

    public GHCIntradaySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    public Result doWork() {
        final ActivitySourcesManager sourcesManager = getOrInitializeActivitySourcesManager();
        final ActivitySourceConnection ghcConnection = getGoogleHealthConnectActivitySourceConnection(sourcesManager);
        if (ghcConnection == null) {
            // TODO: cancel next scheduled tasks because there is not current ghc connection
            // NOTE: currently, the task will be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        final GoogleHealthConnectActivitySource ghcSource = ((GoogleHealthConnectActivitySource) ghcConnection.getActivitySource());
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        final GoogleHealthConnectIntradaySyncOptions syncOptions = buildIntradaySyncOptions();
        ghcSource.syncIntradayMetrics(syncOptions, (result -> {
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
    private GoogleHealthConnectIntradaySyncOptions buildIntradaySyncOptions() {
        final String[] rawIntradayMetrics = getInputData().getStringArray(KEY_INTRADAY_METRICS_ARG);
        final GoogleHealthConnectIntradaySyncOptions.Builder syncOptionsBuilder =
            new GoogleHealthConnectIntradaySyncOptions.Builder();
        for (final String rawIntradayMetric : rawIntradayMetrics) {
            try {
                final FitnessMetricsType metric = FitnessMetricsType.valueOf(rawIntradayMetric);
                if (metric != null) {
                    syncOptionsBuilder.include(metric);
                }
            } catch (Exception e) {}
        }
        return syncOptionsBuilder.build();
    }
}
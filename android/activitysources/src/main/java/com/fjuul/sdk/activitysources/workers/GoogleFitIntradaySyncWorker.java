package com.fjuul.sdk.activitysources.workers;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

public class GoogleFitIntradaySyncWorker extends GoogleFitSyncWorker {
    public static final String KEY_INTRADAY_METRICS_ARG = "INTRADAY_METRICS";

    public GoogleFitIntradaySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    public Result doWork() {
        final ActivitySourcesManager sourcesManager = getOrInitializeActivitySourcesManager();
        final ActivitySourceConnection gfConnection = getGoogleFitActivitySourceConnection(sourcesManager);
        if (gfConnection == null) {
            // TODO: cancel next scheduled tasks because there is not current gf connection
            // NOTE: currently, the task will be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        final GoogleFitActivitySource gfSource = ((GoogleFitActivitySource) gfConnection.getActivitySource());
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        final GoogleFitIntradaySyncOptions syncOptions = buildIntradaySyncOptions();
        gfSource.syncIntradayMetrics(syncOptions, (result -> {
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
    private GoogleFitIntradaySyncOptions buildIntradaySyncOptions() {
        final String[] rawIntradayMetrics = getInputData().getStringArray(KEY_INTRADAY_METRICS_ARG);
        final GoogleFitIntradaySyncOptions.Builder syncOptionsBuilder = new GoogleFitIntradaySyncOptions.Builder();
        for (final String rawIntradayMetric : rawIntradayMetrics) {
            try {
                final FitnessMetricsType metric = FitnessMetricsType.valueOf(rawIntradayMetric);
                if (metric != null) {
                    syncOptionsBuilder.include(metric);
                }
            } catch (Exception e) {}
        }
        return syncOptionsBuilder.setDateRange(LocalDate.now().minusDays(2), LocalDate.now()).build();
    }
}

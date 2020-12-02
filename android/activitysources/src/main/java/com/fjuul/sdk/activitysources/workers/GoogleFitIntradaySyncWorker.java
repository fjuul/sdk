package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.GFIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

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
            // TODO: the task should be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        GoogleFitActivitySource gfSource = ((GoogleFitActivitySource)gfConnection.getActivitySource());
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        GFIntradaySyncOptions syncOptions = buildIntradaySyncOptions();
        gfSource.syncIntradayMetrics(syncOptions, (result -> {
            if (result.isError() && result.getError() instanceof Exception) {
                taskCompletionSource.trySetException((Exception)result.getError());
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
        } catch (InterruptedException e) { }
        return Result.failure();
    }

    @SuppressLint("NewApi")
    private GFIntradaySyncOptions buildIntradaySyncOptions() {
        String[] rawIntradayMetrics = getInputData().getStringArray(KEY_INTRADAY_METRICS_ARG);
        GFIntradaySyncOptions.Builder syncOptionsBuilder = new GFIntradaySyncOptions.Builder();
        for (final String rawIntradayMetric : rawIntradayMetrics) {
            try {
                GFIntradaySyncOptions.METRICS_TYPE metric = GFIntradaySyncOptions.METRICS_TYPE.valueOf(rawIntradayMetric);
                if (metric != null) {
                    syncOptionsBuilder.include(metric);
                }
            } catch (Exception e) { }
        }
        return syncOptionsBuilder.setDateRange(LocalDate.now().minusDays(2), LocalDate.now())
            .build();
    }
}

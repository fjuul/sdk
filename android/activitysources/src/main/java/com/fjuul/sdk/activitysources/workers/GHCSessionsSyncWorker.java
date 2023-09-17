package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectSessionSyncOptions;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

public class GHCSessionsSyncWorker extends GHCSyncWorker {
    public static final String KEY_MIN_SESSION_DURATION_ARG = "MIN_SESSION_DURATION";

    public GHCSessionsSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
            // TODO: the task should be canceled on the next initialization of ActivitySourcesManager
            return Result.success();
        }
        final GoogleHealthConnectActivitySource ghcSource = ((GoogleHealthConnectActivitySource) ghcConnection.getActivitySource());
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        final GoogleHealthConnectSessionSyncOptions syncOptions = buildSessionSyncOptions();
        ghcSource.syncSessions(syncOptions, (result -> {
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
    private GoogleHealthConnectSessionSyncOptions buildSessionSyncOptions() {
        return new GoogleHealthConnectSessionSyncOptions.Builder().setMinimumSessionDuration(Duration.parse(getInputData().getString(KEY_MIN_SESSION_DURATION_ARG)))
            .build();
    }
}

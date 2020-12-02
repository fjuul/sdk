package com.fjuul.sdk.activitysources.workers;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.GFSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

public class GoogleFitSessionsSyncWorker extends GoogleFitSyncWorker {
    public static final String KEY_MIN_SESSION_DURATION_ARG = "MIN_SESSION_DURATION";

    public GoogleFitSessionsSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        final GoogleFitActivitySource gfSource = ((GoogleFitActivitySource)gfConnection.getActivitySource());
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        final GFSessionSyncOptions syncOptions = buildSessionSyncOptions();
        gfSource.syncSessions(syncOptions, (result -> {
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
    private GFSessionSyncOptions buildSessionSyncOptions() {
        return new GFSessionSyncOptions.Builder()
            .setDateRange(LocalDate.now().minusDays(2), LocalDate.now())
            .setMinimumSessionDuration(Duration.parse(getInputData().getString(KEY_MIN_SESSION_DURATION_ARG)))
            .build();
    }
}

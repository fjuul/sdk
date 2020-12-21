package com.fjuul.sdk.activitysources.entities.internal;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.workers.GoogleFitIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GoogleFitSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GoogleFitSyncWorker;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitSyncWorkManager {
    public static final String GF_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_intraday_sync";
    public static final String GF_SESSIONS_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_sessions_sync";

    @NonNull private final WorkManager workManager;
    @NonNull private final String userToken;
    @NonNull private final String userSecret;
    @NonNull private final String apiKey;
    @NonNull private final String baseUrl;
    private volatile boolean intradaySyncWorkEnqueued = false;
    private volatile boolean sessionsSyncWorkEnqueued = false;

    public GoogleFitSyncWorkManager(@NonNull WorkManager workManager,
                                    @NonNull String userToken,
                                    @NonNull String userSecret,
                                    @NonNull String apiKey,
                                    @NonNull String baseUrl) {
        this.workManager = workManager;
        this.userToken = userToken;
        this.userSecret = userSecret;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public static void cancelWorks(@NonNull WorkManager workManager) {
        workManager.cancelUniqueWork(GF_INTRADAY_SYNC_WORK_NAME);
        workManager.cancelUniqueWork(GF_SESSIONS_SYNC_WORK_NAME);
    }

    public synchronized void cancelWorks() {
        cancelWorks(workManager);
        intradaySyncWorkEnqueued = false;
        sessionsSyncWorkEnqueued = false;
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleIntradaySyncWork(@NonNull List<FitnessMetricsType> intradayMetrics) {
        if (intradaySyncWorkEnqueued) {
            return;
        }
        String[] serializedIntradayMetrics = intradayMetrics.stream()
            .map(Enum::toString)
            .toArray(String[]::new);
        Data inputWorkRequestData = buildEssentialInputData()
            .putStringArray(GoogleFitIntradaySyncWorker.KEY_INTRADAY_METRICS_ARG, serializedIntradayMetrics)
            .build();
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(GoogleFitIntradaySyncWorker.class, 1, TimeUnit.HOURS)
            .setConstraints(buildCommonWorkConstraints())
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(inputWorkRequestData)
            .build();
        workManager.enqueueUniquePeriodicWork(GF_INTRADAY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        intradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelIntradaySyncWork() {
        workManager.cancelUniqueWork(GF_INTRADAY_SYNC_WORK_NAME);
        intradaySyncWorkEnqueued = false;
    }

    public synchronized void scheduleSessionsSyncWork(@NonNull Duration minSessionDuration) {
        if (sessionsSyncWorkEnqueued) {
            return;
        }
        String serializedDuration = minSessionDuration.toString();
        Data inputWorkRequestData = buildEssentialInputData()
            .putString(GoogleFitSessionsSyncWorker.KEY_MIN_SESSION_DURATION_ARG, serializedDuration)
            .build();
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(GoogleFitSessionsSyncWorker.class, 1, TimeUnit.HOURS)
            .setConstraints(buildCommonWorkConstraints())
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(inputWorkRequestData)
            .build();
        workManager.enqueueUniquePeriodicWork(GF_SESSIONS_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        sessionsSyncWorkEnqueued = true;
    }

    public synchronized void cancelSessionsSyncWork() {
        workManager.cancelUniqueWork(GF_SESSIONS_SYNC_WORK_NAME);
        sessionsSyncWorkEnqueued = false;
    }

    private Data.Builder buildEssentialInputData() {
        return new Data.Builder()
            .putString(GoogleFitSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(GoogleFitSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(GoogleFitSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(GoogleFitSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    private Constraints buildCommonWorkConstraints() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    }
}

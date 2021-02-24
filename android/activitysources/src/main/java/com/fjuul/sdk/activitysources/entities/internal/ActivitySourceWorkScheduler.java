package com.fjuul.sdk.activitysources.entities.internal;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.workers.GFIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.activitysources.workers.ProfileSyncWorker;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class ActivitySourceWorkScheduler {
    public static final String GF_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_intraday_sync";
    public static final String GF_SESSIONS_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_sessions_sync";
    public static final String PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.profile_sync";

    @NonNull
    private final WorkManager workManager;
    @NonNull
    private final String userToken;
    @NonNull
    private final String userSecret;
    @NonNull
    private final String apiKey;
    @NonNull
    private final String baseUrl;
    private volatile boolean gfIntradaySyncWorkEnqueued = false;
    private volatile boolean gfSessionsSyncWorkEnqueued = false;
    private volatile boolean profileSyncWorkEnqueued = false;

    public ActivitySourceWorkScheduler(@NonNull WorkManager workManager,
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
        workManager.cancelUniqueWork(PROFILE_SYNC_WORK_NAME);
    }

    public synchronized void cancelWorks() {
        cancelGFIntradaySyncWork();
        cancelGFSessionsSyncWork();
        cancelProfileSyncWork();
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleGFIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (gfIntradaySyncWorkEnqueued) {
            return;
        }
        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildEssentialInputData()
            .putStringArray(GFIntradaySyncWorker.KEY_INTRADAY_METRICS_ARG, serializedIntradayMetrics)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GFIntradaySyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GF_INTRADAY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        gfIntradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelGFIntradaySyncWork() {
        workManager.cancelUniqueWork(GF_INTRADAY_SYNC_WORK_NAME);
        gfIntradaySyncWorkEnqueued = false;
    }

    public synchronized void scheduleGFSessionsSyncWork(@NonNull Duration minSessionDuration) {
        if (gfSessionsSyncWorkEnqueued) {
            return;
        }
        final String serializedDuration = minSessionDuration.toString();
        final Data inputWorkRequestData =
            buildEssentialInputData().putString(GFSessionsSyncWorker.KEY_MIN_SESSION_DURATION_ARG, serializedDuration)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GFSessionsSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GF_SESSIONS_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        gfSessionsSyncWorkEnqueued = true;
    }

    public synchronized void cancelGFSessionsSyncWork() {
        workManager.cancelUniqueWork(GF_SESSIONS_SYNC_WORK_NAME);
        gfSessionsSyncWorkEnqueued = false;
    }

    public synchronized void scheduleProfileSyncWork(@NonNull Set<FitnessMetricsType> profileMetrics) {
        if (profileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(profileMetrics);
        final Data inputWorkRequestData =
            buildEssentialInputData().putStringArray(ProfileSyncWorker.KEY_PROFILE_METRICS_ARG, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(ProfileSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager
            .enqueueUniquePeriodicWork(PROFILE_SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest);
        profileSyncWorkEnqueued = true;
    }

    public synchronized void cancelProfileSyncWork() {
        workManager.cancelUniqueWork(PROFILE_SYNC_WORK_NAME);
        profileSyncWorkEnqueued = false;
    }

    private Data.Builder buildEssentialInputData() {
        return new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(GFSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(GFSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(GFSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    private Constraints buildCommonWorkConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    @SuppressLint("NewApi")
    private String[] serializeFitnessMetrics(Set<FitnessMetricsType> metrics) {
        return metrics.stream().map(Enum::toString).toArray(String[]::new);
    }
}

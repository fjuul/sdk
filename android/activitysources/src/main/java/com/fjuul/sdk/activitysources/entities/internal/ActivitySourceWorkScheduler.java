package com.fjuul.sdk.activitysources.entities.internal;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.workers.GFIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GFProfileSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.activitysources.workers.GHCIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GHCProfileSyncWorker;
import com.fjuul.sdk.activitysources.workers.GHCSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GHCSyncWorker;

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
    public static final String GF_PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_profile_sync";
    public static final String GHC_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.ghc_intraday_sync";
    public static final String GHC_SESSIONS_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.ghc_sessions_sync";
    public static final String GHC_PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.ghc_profile_sync";

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
    private volatile boolean gfProfileSyncWorkEnqueued = false;
    private volatile boolean ghcIntradaySyncWorkEnqueued = false;
    private volatile boolean ghcSessionsSyncWorkEnqueued = false;
    private volatile boolean ghcProfileSyncWorkEnqueued = false;

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
        workManager.cancelUniqueWork(GF_PROFILE_SYNC_WORK_NAME);
        workManager.cancelUniqueWork(GHC_INTRADAY_SYNC_WORK_NAME);
        workManager.cancelUniqueWork(GHC_SESSIONS_SYNC_WORK_NAME);
        workManager.cancelUniqueWork(GHC_PROFILE_SYNC_WORK_NAME);
    }

    public synchronized void cancelWorks() {
        cancelGFIntradaySyncWork();
        cancelGFSessionsSyncWork();
        cancelGFProfileSyncWork();
        cancelGHCIntradaySyncWork();
        cancelGHCSessionsSyncWork();
        cancelGHCProfileSyncWork();
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleGFIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (gfIntradaySyncWorkEnqueued) {
            return;
        }
        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildGFEssentialInputData()
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
            buildGFEssentialInputData().putString(GFSessionsSyncWorker.KEY_MIN_SESSION_DURATION_ARG, serializedDuration)
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

    public synchronized void scheduleGFProfileSyncWork(@NonNull Set<FitnessMetricsType> profileMetrics) {
        if (gfProfileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(profileMetrics);
        final Data inputWorkRequestData =
            buildGFEssentialInputData().putStringArray(GFProfileSyncWorker.KEY_PROFILE_METRICS_ARG, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GFProfileSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GF_PROFILE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        gfProfileSyncWorkEnqueued = true;
    }

    public synchronized void cancelGFProfileSyncWork() {
        workManager.cancelUniqueWork(GF_PROFILE_SYNC_WORK_NAME);
        gfProfileSyncWorkEnqueued = false;
    }

    private Data.Builder buildGFEssentialInputData() {
        return new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(GFSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(GFSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(GFSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleGHCIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (ghcIntradaySyncWorkEnqueued) {
            return;
        }
        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildGHCEssentialInputData()
            .putStringArray(GHCIntradaySyncWorker.KEY_INTRADAY_METRICS_ARG, serializedIntradayMetrics)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GHCIntradaySyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GHC_INTRADAY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        ghcIntradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelGHCIntradaySyncWork() {
        workManager.cancelUniqueWork(GHC_INTRADAY_SYNC_WORK_NAME);
        ghcIntradaySyncWorkEnqueued = false;
    }

    public synchronized void scheduleGHCSessionsSyncWork(@NonNull Duration minSessionDuration) {
        if (ghcSessionsSyncWorkEnqueued) {
            return;
        }
        final String serializedDuration = minSessionDuration.toString();
        final Data inputWorkRequestData = buildGHCEssentialInputData()
            .putString(GHCSessionsSyncWorker.KEY_MIN_SESSION_DURATION_ARG, serializedDuration)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GHCSessionsSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GHC_SESSIONS_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        ghcSessionsSyncWorkEnqueued = true;
    }

    public synchronized void cancelGHCSessionsSyncWork() {
        workManager.cancelUniqueWork(GHC_SESSIONS_SYNC_WORK_NAME);
        ghcSessionsSyncWorkEnqueued = false;
    }

    public synchronized void scheduleGHCProfileSyncWork(@NonNull Set<FitnessMetricsType> profileMetrics) {
        if (ghcProfileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(profileMetrics);
        final Data inputWorkRequestData =
            buildGHCEssentialInputData().putStringArray(GHCProfileSyncWorker.KEY_PROFILE_METRICS_ARG, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(GHCProfileSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(GHC_PROFILE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        ghcProfileSyncWorkEnqueued = true;
    }

    public synchronized void cancelGHCProfileSyncWork() {
        workManager.cancelUniqueWork(GHC_PROFILE_SYNC_WORK_NAME);
        ghcProfileSyncWorkEnqueued = false;
    }

    private Data.Builder buildGHCEssentialInputData() {
        return new Data.Builder().putString(GHCSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(GHCSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(GHCSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(GHCSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    private Constraints buildCommonWorkConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    @SuppressLint("NewApi")
    private String[] serializeFitnessMetrics(Set<FitnessMetricsType> metrics) {
        return metrics.stream().map(Enum::toString).toArray(String[]::new);
    }
}

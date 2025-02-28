package com.fjuul.sdk.activitysources.entities.internal;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.workers.GFIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GFProfileSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.activitysources.workers.HCIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.HCProfileSyncWorker;
import com.fjuul.sdk.activitysources.workers.HCSyncWorker;

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
    public static final String HC_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_intraday_sync";
    public static final String HC_PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_profile_sync";

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
    private volatile boolean hcIntradaySyncWorkEnqueued = false;
    private volatile boolean hcProfileSyncWorkEnqueued = false;

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
        workManager.cancelUniqueWork(HC_INTRADAY_SYNC_WORK_NAME);
        workManager.cancelUniqueWork(HC_PROFILE_SYNC_WORK_NAME);
    }

    public synchronized void cancelWorks() {
        cancelGFIntradaySyncWork();
        cancelGFSessionsSyncWork();
        cancelGFProfileSyncWork();
        cancelHCIntradaySyncWork();
        cancelHCProfileSyncWork();
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
    public synchronized void scheduleHCIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (hcIntradaySyncWorkEnqueued) {
            return;
        }
        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildHCEssentialInputData()
            .putStringArray(HCIntradaySyncWorker.KEY_INTRADAY_METRICS_ARG, serializedIntradayMetrics)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(HCIntradaySyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(HC_INTRADAY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        hcIntradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelHCIntradaySyncWork() {
        workManager.cancelUniqueWork(HC_INTRADAY_SYNC_WORK_NAME);
        hcIntradaySyncWorkEnqueued = false;
    }

    public synchronized void scheduleHCProfileSyncWork(@NonNull Set<FitnessMetricsType> profileMetrics) {
        if (hcProfileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(profileMetrics);
        final Data inputWorkRequestData =
            buildHCEssentialInputData().putStringArray(HCProfileSyncWorker.KEY_PROFILE_METRICS_ARG, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            new PeriodicWorkRequest.Builder(HCProfileSyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputWorkRequestData)
                .build();
        workManager.enqueueUniquePeriodicWork(HC_PROFILE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest);
        hcProfileSyncWorkEnqueued = true;
    }

    public synchronized void cancelHCProfileSyncWork() {
        workManager.cancelUniqueWork(HC_PROFILE_SYNC_WORK_NAME);
        hcProfileSyncWorkEnqueued = false;
    }

    private Data.Builder buildHCEssentialInputData() {
        return new Data.Builder().putString(HCSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(HCSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(HCSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(HCSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    private Constraints buildCommonWorkConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    @SuppressLint("NewApi")
    private String[] serializeFitnessMetrics(Set<FitnessMetricsType> metrics) {
        return metrics.stream().map(Enum::toString).toArray(String[]::new);
    }
}

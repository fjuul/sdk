package com.fjuul.sdk.activitysources.entities.internal;

import java.util.Set;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class BackgroundWorkManager {
    @NonNull
    private final ActivitySourcesManagerConfig config;
    @NonNull
    private final GoogleFitSyncWorkManager gfSyncWorkManager;

    public BackgroundWorkManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull GoogleFitSyncWorkManager gfSyncWorkManager) {
        this.config = config;
        this.gfSyncWorkManager = gfSyncWorkManager;
    }

    @SuppressLint("NewApi")
    public void configureBackgroundGFSyncWorks() {
        switch (config.getGfIntradayBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelIntradaySyncWork();
                break;
            }
            case ENABLED: {
                final Set<FitnessMetricsType> intradayMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isIntradayMetricType)
                    .collect(Collectors.toSet());
                if (intradayMetrics.isEmpty()) {
                    gfSyncWorkManager.cancelIntradaySyncWork();
                } else {
                    gfSyncWorkManager.scheduleIntradaySyncWork(intradayMetrics);
                }
                break;
            }
        }
        switch (config.getGfSessionsBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelSessionsSyncWork();
                break;
            }
            case ENABLED: {
                if (config.getCollectableFitnessMetrics().contains(FitnessMetricsType.WORKOUTS)) {
                    gfSyncWorkManager.scheduleSessionsSyncWork(config.getGfSessionsBackgroundSyncMinSessionDuration());
                } else {
                    gfSyncWorkManager.cancelSessionsSyncWork();
                }
                break;
            }
        }
    }

    public void cancelBackgroundGFSyncWorks() {
        gfSyncWorkManager.cancelWorks();
    }
}

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
    private final ActivitySourceWorkScheduler activitySourceWorkScheduler;

    public BackgroundWorkManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull ActivitySourceWorkScheduler activitySourceWorkScheduler) {
        this.config = config;
        this.activitySourceWorkScheduler = activitySourceWorkScheduler;
    }

    @SuppressLint("NewApi")
    public void configureBackgroundGFSyncWorks() {
        switch (config.getGoogleFitIntradayBackgroundSyncMode()) {
            case DISABLED: {
                activitySourceWorkScheduler.cancelGFIntradaySyncWork();
                break;
            }
            case ENABLED: {
                final Set<FitnessMetricsType> intradayMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isIntradayMetricType)
                    .collect(Collectors.toSet());
                if (intradayMetrics.isEmpty()) {
                    activitySourceWorkScheduler.cancelGFIntradaySyncWork();
                } else {
                    activitySourceWorkScheduler.scheduleGFIntradaySyncWork(intradayMetrics);
                }
                break;
            }
        }
        switch (config.getGoogleFitSessionsBackgroundSyncMode()) {
            case DISABLED: {
                activitySourceWorkScheduler.cancelGFSessionsSyncWork();
                break;
            }
            case ENABLED: {
                if (config.getCollectableFitnessMetrics().contains(FitnessMetricsType.WORKOUTS)) {
                    activitySourceWorkScheduler
                        .scheduleGFSessionsSyncWork(config.getGoogleFitSessionsBackgroundSyncMinSessionDuration());
                } else {
                    activitySourceWorkScheduler.cancelGFSessionsSyncWork();
                }
                break;
            }
        }
    }

    public void cancelBackgroundGFSyncWorks() {
        activitySourceWorkScheduler.cancelWorks();
    }
}

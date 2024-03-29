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
    private final ActivitySourceWorkScheduler workScheduler;

    public BackgroundWorkManager(@NonNull ActivitySourcesManagerConfig config,
        @NonNull ActivitySourceWorkScheduler workScheduler) {
        this.config = config;
        this.workScheduler = workScheduler;
    }

    @SuppressLint("NewApi")
    public void configureGFSyncWorks() {
        switch (config.getGoogleFitIntradayBackgroundSyncMode()) {
            case DISABLED: {
                workScheduler.cancelGFIntradaySyncWork();
                break;
            }
            case ENABLED: {
                final Set<FitnessMetricsType> intradayMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isIntradayMetricType)
                    .collect(Collectors.toSet());
                if (intradayMetrics.isEmpty()) {
                    workScheduler.cancelGFIntradaySyncWork();
                } else {
                    workScheduler.scheduleGFIntradaySyncWork(intradayMetrics);
                }
                break;
            }
        }
        switch (config.getGoogleFitSessionsBackgroundSyncMode()) {
            case DISABLED: {
                workScheduler.cancelGFSessionsSyncWork();
                break;
            }
            case ENABLED: {
                if (config.getCollectableFitnessMetrics().contains(FitnessMetricsType.WORKOUTS)) {
                    workScheduler
                        .scheduleGFSessionsSyncWork(config.getGoogleFitSessionsBackgroundSyncMinSessionDuration());
                } else {
                    workScheduler.cancelGFSessionsSyncWork();
                }
                break;
            }
        }
    }

    public void cancelGFSyncWorks() {
        workScheduler.cancelGFIntradaySyncWork();
        workScheduler.cancelGFSessionsSyncWork();
    }

    @SuppressLint("NewApi")
    public void configureProfileSyncWork() {
        switch (config.getProfileBackgroundSyncMode()) {
            case DISABLED:
                workScheduler.cancelProfileSyncWork();
                break;
            case ENABLED: {
                final Set<FitnessMetricsType> profileMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isProfileMetricType)
                    .collect(Collectors.toSet());
                if (profileMetrics.isEmpty()) {
                    workScheduler.cancelProfileSyncWork();
                } else {
                    workScheduler.scheduleProfileSyncWork(profileMetrics);
                }
                break;
            }
        }
    }

    public void cancelProfileSyncWork() {
        workScheduler.cancelProfileSyncWork();
    }
}

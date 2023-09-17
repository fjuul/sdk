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
    public void configureGFProfileSyncWork() {
        switch (config.getGoogleFitProfileBackgroundSyncMode()) {
            case DISABLED:
                workScheduler.cancelGFProfileSyncWork();
                break;
            case ENABLED: {
                final Set<FitnessMetricsType> profileMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isProfileMetricType)
                    .collect(Collectors.toSet());
                if (profileMetrics.isEmpty()) {
                    workScheduler.cancelGFProfileSyncWork();
                } else {
                    workScheduler.scheduleGFProfileSyncWork(profileMetrics);
                }
                break;
            }
        }
    }

    public void cancelGFProfileSyncWork() {
        workScheduler.cancelGFProfileSyncWork();
    }

    @SuppressLint("NewApi")
    public void configureGHCSyncWorks() {
        switch (config.getGoogleHealthConnectIntradayBackgroundSyncMode()) {
            case DISABLED: {
                workScheduler.cancelGHCIntradaySyncWork();
                break;
            }
            case ENABLED: {
                final Set<FitnessMetricsType> intradayMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isIntradayMetricType)
                    .collect(Collectors.toSet());
                if (intradayMetrics.isEmpty()) {
                    workScheduler.cancelGHCIntradaySyncWork();
                } else {
                    workScheduler.scheduleGHCIntradaySyncWork(intradayMetrics);
                }
                break;
            }
        }
        switch (config.getGoogleHealthConnectSessionsBackgroundSyncMode()) {
            case DISABLED: {
                workScheduler.cancelGHCSessionsSyncWork();
                break;
            }
            case ENABLED: {
                if (config.getCollectableFitnessMetrics().contains(FitnessMetricsType.WORKOUTS)) {
                    workScheduler
                        .scheduleGHCSessionsSyncWork(config.getGoogleHealthConnectSessionsBackgroundSyncMinSessionDuration());
                } else {
                    workScheduler.cancelGHCSessionsSyncWork();
                }
                break;
            }
        }
    }

    public void cancelGHCSyncWorks() {
        workScheduler.cancelGHCIntradaySyncWork();
        workScheduler.cancelGHCSessionsSyncWork();
    }

    @SuppressLint("NewApi")
    public void configureGHCProfileSyncWork() {
        switch (config.getGoogleHealthConnectProfileBackgroundSyncMode()) {
            case DISABLED:
                workScheduler.cancelGHCProfileSyncWork();
                break;
            case ENABLED: {
                final Set<FitnessMetricsType> profileMetrics = config.getCollectableFitnessMetrics()
                    .stream()
                    .filter(FitnessMetricsType::isProfileMetricType)
                    .collect(Collectors.toSet());
                if (profileMetrics.isEmpty()) {
                    workScheduler.cancelGHCProfileSyncWork();
                } else {
                    workScheduler.scheduleGHCProfileSyncWork(profileMetrics);
                }
                break;
            }
        }
    }

    public void cancelGHCProfileSyncWork() {
        workScheduler.cancelGHCProfileSyncWork();
    }
}

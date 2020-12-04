package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public class BackgroundWorkManager {
    @NonNull private final ActivitySourcesManagerConfig config;
    @NonNull private final GoogleFitSyncWorkManager gfSyncWorkManager;

    public BackgroundWorkManager(@NonNull ActivitySourcesManagerConfig config,
                                 @NonNull GoogleFitSyncWorkManager gfSyncWorkManager) {
        this.config = config;
        this.gfSyncWorkManager = gfSyncWorkManager;
    }

    public void configureBackgroundGFSyncWorks() {
        switch (config.getGfIntradayBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelIntradaySyncWork();
                break;
            }
            case ENABLED: {
                gfSyncWorkManager.scheduleIntradaySyncWork(config.getGfIntradayBackgroundSyncMetrics());
                break;
            }
        }
        switch (config.getGfSessionsBackgroundSyncMode()) {
            case DISABLED: {
                gfSyncWorkManager.cancelSessionsSyncWork();
                break;
            }
            case ENABLED: {
                gfSyncWorkManager.scheduleSessionsSyncWork(config.getGfSessionsBackgroundSyncMinSessionDuration());
                break;
            }
        }
    }

    public void cancelBackgroundGFSyncWorks() {
        gfSyncWorkManager.cancelWorks();
    }
}

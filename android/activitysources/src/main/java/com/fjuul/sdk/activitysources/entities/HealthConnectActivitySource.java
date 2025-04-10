package com.fjuul.sdk.activitysources.entities;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata.HCSyncMetadataStore;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The ActivitySource class for the Health Connect tracker. This is a local activity source (i.e. data that needs to be
 * synced will be taken from the user device).
 */
public class HealthConnectActivitySource extends ActivitySource {
    private static volatile HealthConnectActivitySource instance;

    private final @NonNull Set<FitnessMetricsType> collectableFitnessMetrics;
    private final @NonNull ActivitySourcesService sourcesService;
    private final @NonNull Context context;
    private final @NonNull HCSyncMetadataStore syncMetadataStore;
    private volatile @Nullable Date lowerDateBoundary;

    static synchronized void initialize(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig sourcesManagerConfig) {
        final Context context = client.getAppContext();
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final HCSyncMetadataStore syncMetadataStore = new HCSyncMetadataStore(client.getStorage());
        instance = new HealthConnectActivitySource(
            sourcesManagerConfig.getCollectableFitnessMetrics(),
            sourcesService,
            context,
            syncMetadataStore
        );
    }

    /**
     * Return the initialized and configured instance of HealthConnectActivitySource. This method should be invoked
     * after ActivitySourcesManager.initialize. Otherwise, it throws IllegalStateException.
     *
     * @throws IllegalStateException if not initialized yet
     * @return instance of HealthConnectActivitySource
     */
    @NonNull
    public static HealthConnectActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "You must initialize ActivitySourceManager first before use HealthConnectActivitySource");
        }
        return instance;
    }

    public HealthConnectActivitySource(@NonNull Set<FitnessMetricsType> collectableFitnessMetrics,
        @NonNull ActivitySourcesService sourcesService,
        @NonNull Context context,
        @NonNull HCSyncMetadataStore syncMetadataStore) {
        this.collectableFitnessMetrics = collectableFitnessMetrics;
        this.sourcesService = sourcesService;
        this.context = context;
        this.syncMetadataStore = syncMetadataStore;
    }

    @Override
    @NonNull
    protected TrackerValue getTrackerValue() {
        return TrackerValue.HEALTH_CONNECT;
    }

    @NonNull
    public CompletableFuture<Void> syncData(@NonNull Date startDate, @NonNull Date endDate) {
        // TODO: Implement data sync using HealthConnect API
        return CompletableFuture.completedFuture(null);
    }
}

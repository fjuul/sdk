package com.fjuul.sdk.activitysources.entities;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fjuul.sdk.activitysources.entities.internal.HCClientWrapper;
import com.fjuul.sdk.activitysources.entities.internal.HCDataManager;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.utils.Logger;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HealthConnectActivitySource extends ActivitySource {
    private static final ExecutorService sharedSequentialExecutor = createSequentialSingleCachedExecutor();

    private static volatile HealthConnectActivitySource instance;
    private final @NonNull ActivitySourcesService sourcesService;
    private final @NonNull ApiClient apiClient;
    private final @NonNull HCClientWrapper clientWrapper;
    private final @NonNull ExecutorService localSequentialBackgroundExecutor;

    static synchronized void initialize(@NonNull ApiClient client, @NonNull ActivitySourcesManagerConfig config) {
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final Set<FitnessMetricsType> collectableFitnessMetrics = config.getCollectableFitnessMetrics();
        instance = new HealthConnectActivitySource(collectableFitnessMetrics,
            sourcesService,
            client,
            sharedSequentialExecutor);
    }

    private HealthConnectActivitySource(@NonNull Set<FitnessMetricsType> collectableFitnessMetrics,
                                        @NonNull ActivitySourcesService sourcesService,
                                        @NonNull ApiClient apiClient,
                                        @NonNull ExecutorService localSequentialBackgroundExecutor) {
        this.sourcesService = sourcesService;
        this.apiClient = apiClient;
        Context context = apiClient.getAppContext();
        this.clientWrapper = new HCClientWrapper(context);
        this.localSequentialBackgroundExecutor = localSequentialBackgroundExecutor;
    }

    /**
     * Puts the task of synchronizing intraday data in a sequential execution queue (i.e., only one sync task can be
     * executed at a time) and will execute it when it comes to its turn. The synchronization result is available in the
     * callback.<br>
     * The task is atomic, so it will either succeed for all the specified types of metrics, or it will not succeed at
     * all.
     *
     * @param callback callback for the result
     */
    public void syncIntradayMetrics(@NonNull final HealthConnectIntradaySyncOptions options,
        @Nullable final Callback<Void> callback) {
        Logger.get().d("Syncing intraday metrics");
        HCDataManager dataManager = new HCDataManager(clientWrapper, sourcesService, apiClient);
        performTaskAlongWithCallback(() -> dataManager.syncIntradayMetrics(options), callback);
    }

    /**
     * Puts the task of synchronizing the user profile from Health Connect in a sequential execution queue (i.e., only one
     * sync task can be executed at a time) and will execute it when it comes to its turn. The synchronization result is
     * available in the callback.<br>
     * The task is atomic, so it will either succeed for all the specified types of metrics, or it will not succeed at
     * all.
     *
     * @param callback callback for the result
     */
    public void syncProfile(@NonNull final HealthConnectProfileSyncOptions options,
        @Nullable final Callback<Void> callback) {
        Logger.get().d("Syncing profile");
        HCDataManager dataManager = new HCDataManager(clientWrapper, sourcesService, apiClient);
        performTaskAlongWithCallback(() -> dataManager.syncProfile(options), callback);
    }

    @NonNull
    public static HealthConnectActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "You must initialize ActivitySourceManager before use of HealthConnectActivitySource");
        }
        return instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.HEALTH_CONNECT;
    }

    private <T> void performTaskAlongWithCallback(@NonNull Supplier<Task<T>> taskSupplier,
        @Nullable Callback<T> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                T taskResult = Tasks.await(taskSupplier.get());
                Result<T> result = Result.value(taskResult);
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (ExecutionException | InterruptedException exc) {
                if (callback == null) {
                    return;
                }
                Throwable throwableToPropagate = exc;
                if (exc instanceof ExecutionException && exc.getCause() != null) {
                    throwableToPropagate = exc.getCause();
                }
                Result<T> errorResult = Result.error(throwableToPropagate);
                callback.onResult(errorResult);
            }
        });
    }

    private static ExecutorService createSequentialSingleCachedExecutor() {
        // NOTE: this solution works only for single thread (do not edit maximumPoolSize)
        return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }
}

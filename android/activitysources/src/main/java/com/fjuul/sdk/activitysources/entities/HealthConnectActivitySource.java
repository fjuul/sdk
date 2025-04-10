package com.fjuul.sdk.activitysources.entities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;

import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataManager;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataManagerBuilder;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataUtils;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata.HCSyncMetadataStore;
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.HealthConnectPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.FjuulException;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.O)
public class HealthConnectActivitySource extends ActivitySource {
    private static final ExecutorService sharedSequentialExecutor = createSequentialSingleCachedExecutor();

    private static volatile HealthConnectActivitySource instance;
    private final Context context;
    private final ApiClient apiClient;
    private final HCDataManager dataManager;
    private final ExecutorService localSequentialBackgroundExecutor;
    private volatile Date lowerDateBoundary;

    static synchronized void initialize(@NonNull ApiClient client,
        @NonNull ActivitySourcesManagerConfig sourcesManagerConfig) {
        final Context context = client.getAppContext();
        final ActivitySourcesService sourcesService = new ActivitySourcesService(client);
        final HCDataUtils dataUtils = new HCDataUtils();
        final HCSyncMetadataStore syncMetadataStore = new HCSyncMetadataStore(client.getStorage());
        final HCDataManagerBuilder dataManagerBuilder =
            new HCDataManagerBuilder(context, dataUtils, sourcesService);
        instance = new HealthConnectActivitySource(context, client, dataManagerBuilder, sharedSequentialExecutor);
    }

    @NonNull
    public static HealthConnectActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "You must initialize ActivitySourceManager first before use HealthConnectActivitySource");
        }
        return instance;
    }

    private HealthConnectActivitySource(@NonNull Context context, @NonNull ApiClient apiClient,
        @NonNull HCDataManagerBuilder dataManagerBuilder,
        @NonNull ExecutorService localSequentialBackgroundExecutor) {
        this.context = context;
        this.apiClient = apiClient;
        this.dataManager = dataManagerBuilder.build();
        this.localSequentialBackgroundExecutor = localSequentialBackgroundExecutor;
    }

    public boolean isAvailable() {
        return HealthConnectClient.isAvailable(context);
    }

    @NonNull
    public Intent buildIntentRequestingHealthConnectPermissions() {
        return HealthConnectClient.getOrCreate(context)
            .permissionController
            .createRequestPermissionIntent(dataManager.getRequiredPermissions());
    }

    public void handleHealthConnectPermissionResult(@NonNull Intent data, @NonNull Callback<Void> callback) {
        Set<String> grantedPermissions = data.getStringArraySetExtra("granted_permissions");
        if (grantedPermissions == null || !grantedPermissions.containsAll(dataManager.getRequiredPermissions())) {
            callback.onResult(Result.error(new HealthConnectPermissionsNotGrantedException()));
            return;
        }

        ActivitySourcesService sourcesService = new ActivitySourcesService(apiClient);
        sourcesService.connect(getTrackerValue().getValue()).enqueue((call, result) -> {
            if (result.isError()) {
                callback.onResult(Result.error(result.getError()));
                return;
            }
            callback.onResult(Result.value(null));
        });
    }

    public void syncIntradayMetrics(@NonNull HealthConnectIntradaySyncOptions options,
        @NonNull Callback<Void> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                dataManager.readIntradayMetrics(
                    options.getMetrics().iterator().next().toString().toLowerCase(),
                    options.getStartTime(),
                    options.getEndTime()
                ).thenCompose(dataPoints -> {
                    HCDataPointsBatch batch = new HCDataPointsBatch(dataPoints);
                    return apiClient.uploadHealthConnectData(apiClient.getUserToken(), batch);
                }).whenComplete((v, e) -> {
                    if (e != null) {
                        callback.onResult(Result.error(new FjuulException(e.getMessage())));
                    } else {
                        callback.onResult(Result.value(null));
                    }
                });
            } catch (Exception e) {
                callback.onResult(Result.error(new FjuulException(e.getMessage())));
            }
        });
    }

    public void syncSessions(@NonNull HealthConnectSessionSyncOptions options,
        @NonNull Callback<Void> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                dataManager.readExerciseSessions(options.getStartTime(), options.getEndTime())
                    .thenCompose(sessions -> apiClient.uploadHealthConnectSessions(apiClient.getUserToken(), sessions))
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            callback.onResult(Result.error(new FjuulException(e.getMessage())));
                        } else {
                            callback.onResult(Result.value(null));
                        }
                    });
            } catch (Exception e) {
                callback.onResult(Result.error(new FjuulException(e.getMessage())));
            }
        });
    }

    public void syncProfile(@NonNull HealthConnectProfileSyncOptions options,
        @NonNull Callback<Boolean> callback) {
        localSequentialBackgroundExecutor.execute(() -> {
            try {
                dataManager.readWeight()
                    .thenCompose(weightPoints -> {
                        HCDataPointsBatch batch = new HCDataPointsBatch(weightPoints);
                        return apiClient.uploadHealthConnectData(apiClient.getUserToken(), batch);
                    })
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            callback.onResult(Result.error(new FjuulException(e.getMessage())));
                        } else {
                            callback.onResult(Result.value(true));
                        }
                    });
            } catch (Exception e) {
                callback.onResult(Result.error(new FjuulException(e.getMessage())));
            }
        });
    }

    @Override
    @NonNull
    protected TrackerValue getTrackerValue() {
        return TrackerValue.HEALTH_CONNECT;
    }

    void setLowerDateBoundary(@NonNull Date lowerDateBoundary) {
        this.lowerDateBoundary = lowerDateBoundary;
    }

    private static ExecutorService createSequentialSingleCachedExecutor() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }
} 
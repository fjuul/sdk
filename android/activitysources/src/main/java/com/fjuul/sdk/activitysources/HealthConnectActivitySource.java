package com.fjuul.sdk.activitysources;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;

import com.fjuul.sdk.activitysources.entities.HealthConnectIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.HealthConnectSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.HealthConnectProfileSyncOptions;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCSessionBundle;
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.HealthConnectPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.managers.HCDataManager;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.FitnessMetricsType;
import com.fjuul.sdk.core.exceptions.FjuulException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RequiresApi(api = Build.VERSION_CODES.O)
public class HealthConnectActivitySource {
    private static volatile HealthConnectActivitySource instance;
    private final Context context;
    private final ApiClient apiClient;
    private final HCDataManager dataManager;

    private HealthConnectActivitySource(@NonNull Context context, @NonNull ApiClient apiClient) {
        this.context = context;
        this.apiClient = apiClient;
        this.dataManager = new HCDataManager(context);
    }

    public static void initialize(@NonNull Context context, @NonNull ApiClient apiClient) {
        if (instance == null) {
            synchronized (HealthConnectActivitySource.class) {
                if (instance == null) {
                    instance = new HealthConnectActivitySource(context, apiClient);
                }
            }
        }
    }

    @NonNull
    public static HealthConnectActivitySource getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HealthConnectActivitySource must be initialized first");
        }
        return instance;
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

    public CompletableFuture<Void> handleHealthConnectPermissionResult(@NonNull Intent data) {
        Set<String> grantedPermissions = data.getStringArraySetExtra("granted_permissions");
        if (grantedPermissions == null || !grantedPermissions.containsAll(dataManager.getRequiredPermissions())) {
            return CompletableFuture.failedFuture(new HealthConnectPermissionsNotGrantedException());
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> syncIntradayMetrics(@NonNull HealthConnectIntradaySyncOptions options) {
        List<CompletableFuture<List<HCDataPoint>>> futures = new ArrayList<>();
        
        for (FitnessMetricsType metric : options.getMetrics()) {
            futures.add(dataManager.readIntradayMetrics(
                metric.toString().toLowerCase(),
                options.getStartTime(),
                options.getEndTime()
            ));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenCompose(v -> {
                List<HCDataPoint> allDataPoints = new ArrayList<>();
                for (CompletableFuture<List<HCDataPoint>> future : futures) {
                    allDataPoints.addAll(future.join());
                }
                return apiClient.uploadHealthConnectData(apiClient.getUserToken(), new HCDataPointsBatch(allDataPoints));
            });
    }

    public CompletableFuture<Void> syncSessions(@NonNull HealthConnectSessionSyncOptions options) {
        return dataManager.readExerciseSessions(options.getStartTime(), options.getEndTime())
            .thenCompose(sessions -> apiClient.uploadHealthConnectSessions(apiClient.getUserToken(), sessions));
    }

    public CompletableFuture<Void> syncProfile(@NonNull HealthConnectProfileSyncOptions options) {
        List<CompletableFuture<List<HCDataPoint>>> futures = new ArrayList<>();
        
        for (FitnessMetricsType metric : options.getMetrics()) {
            switch (metric) {
                case HEIGHT:
                    futures.add(dataManager.readHeight());
                    break;
                case WEIGHT:
                    futures.add(dataManager.readWeight());
                    break;
                default:
                    return CompletableFuture.failedFuture(new FjuulException("Unsupported profile metric: " + metric));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenCompose(v -> {
                List<HCDataPoint> allDataPoints = new ArrayList<>();
                for (CompletableFuture<List<HCDataPoint>> future : futures) {
                    allDataPoints.addAll(future.join());
                }
                return apiClient.uploadHealthConnectData(apiClient.getUserToken(), new HCDataPointsBatch(allDataPoints));
            });
    }
} 
package com.fjuul.sdk.activitysources.managers;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCSessionBundle;
import com.fjuul.sdk.activitysources.utils.healthconnect.HCDataUtils;
import com.fjuul.sdk.core.exceptions.FjuulException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RequiresApi(api = Build.VERSION_CODES.O)
public class HCDataManager {
    private final HealthConnectClient healthConnectClient;

    public HCDataManager(@NonNull Context context) {
        healthConnectClient = HealthConnectClient.getOrCreate(context);
    }

    public CompletableFuture<List<HCDataPoint>> readIntradayMetrics(String metricType, Instant startTime, Instant endTime) {
        TimeRangeFilter timeFilter = new TimeRangeFilter.Builder()
            .setStartTime(startTime)
            .setEndTime(endTime)
            .build();

        ReadRecordsRequest<?> request = buildReadRequest(metricType, timeFilter);
        if (request == null) {
            return CompletableFuture.failedFuture(new FjuulException("Unsupported metric type: " + metricType));
        }

        return healthConnectClient.readRecords(request)
            .thenApply(response -> HCDataUtils.convertAggregationResultToDataPoints(response.getRecords(), metricType));
    }

    public CompletableFuture<List<HCSessionBundle>> readExerciseSessions(Instant startTime, Instant endTime) {
        TimeRangeFilter timeFilter = new TimeRangeFilter.Builder()
            .setStartTime(startTime)
            .setEndTime(endTime)
            .build();

        ReadRecordsRequest<ExerciseSessionRecord> request = new ReadRecordsRequest.Builder<>(ExerciseSessionRecord.class)
            .setTimeRangeFilter(timeFilter)
            .build();

        return healthConnectClient.readRecords(request)
            .thenApply(response -> HCDataUtils.convertExerciseSessions(response.getRecords()));
    }

    public CompletableFuture<List<HCDataPoint>> readWeight() {
        ReadRecordsRequest<WeightRecord> request = new ReadRecordsRequest.Builder<>(WeightRecord.class)
            .build();

        return healthConnectClient.readRecords(request)
            .thenApply(response -> HCDataUtils.convertWeightRecords(response.getRecords()));
    }

    public CompletableFuture<List<HCDataPoint>> readHeight() {
        ReadRecordsRequest<HeightRecord> request = new ReadRecordsRequest.Builder<>(HeightRecord.class)
            .build();

        return healthConnectClient.readRecords(request)
            .thenApply(response -> HCDataUtils.convertHeightRecords(response.getRecords()));
    }

    public Set<String> getRequiredPermissions() {
        return Set.of(
            HealthPermission.getReadPermission(ExerciseSessionRecord.class),
            HealthPermission.getReadPermission(WeightRecord.class),
            HealthPermission.getReadPermission(HeightRecord.class),
            HealthPermission.getReadPermission(StepsRecord.class),
            HealthPermission.getReadPermission(HeartRateRecord.class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord.class)
        );
    }

    private ReadRecordsRequest<?> buildReadRequest(String metricType, TimeRangeFilter timeFilter) {
        switch (metricType) {
            case "calories":
                return new ReadRecordsRequest.Builder<>(ActiveCaloriesBurnedRecord.class)
                    .setTimeRangeFilter(timeFilter)
                    .build();
            case "steps":
                return new ReadRecordsRequest.Builder<>(StepsRecord.class)
                    .setTimeRangeFilter(timeFilter)
                    .build();
            case "heart_rate":
                return new ReadRecordsRequest.Builder<>(HeartRateRecord.class)
                    .setTimeRangeFilter(timeFilter)
                    .build();
            default:
                return null;
        }
    }
} 
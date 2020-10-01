package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.core.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private GFHistoryClientWrapper client;
    private GFDataUtils gfUtils;
    private GFSyncMetadataStore gfSyncMetadataStore;

    public GoogleFitDataManager(GFHistoryClientWrapper client, GFDataUtils gfUtils, GFSyncMetadataStore gfSyncMetadataStore) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
    }

    @SuppressLint("NewApi")
    public void syncCalories(LocalDate start, LocalDate end) {
        // TODO: throw if end is future!
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        client.getCalories(gfQueryDates.first, gfQueryDates.second).continueWith((getCaloriesTask) -> {
            if (!getCaloriesTask.isSuccessful()) {
                // TODO: invoke callback with exception
                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
            }
            List<GFCalorieDataPoint> calories = getCaloriesTask.getResult();
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end);
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, calories, Duration.ofMinutes(30));
            Stream<GFDataPointsBatch<GFCalorieDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
                .collect(Collectors.toList());
            if (notSyncedBatches.isEmpty()) {
                // TODO: invoke callback with no result (or empty metadata) ?
                return null;
            }
            // TODO: send the data to the back-end side => add service for that (consider retry here)
            notSyncedBatches.forEach(batch -> {
                this.gfSyncMetadataStore.saveSyncMetadataOfCalories(batch);
            });
            // TODO: pass metadata to the callback ???
            return null;
        });
    }
}

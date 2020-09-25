package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GoogleFitDataManager {
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
    public void syncCalories(Date start, Date end) {
        client.getCalories(start, end).continueWith((getCaloriesTask) -> {
            if (!getCaloriesTask.isSuccessful()) {
                throw  new Error("Couldn't get calories from GoogleFit Api");
            }
            List<GFCalorieDataPoint> calories = getCaloriesTask.getResult();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(start, end, calories, Duration.ofMinutes(30));
            Stream<GFDataPointsBatch<GFCalorieDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
                .collect(Collectors.toList());
            if (notSyncedBatches.isEmpty()) {
                // TODO: invoke callback with no result (or empty metadata) ?
                return null;
            }
            // TODO: combine all batches's calories into one list of GFCalorieDataPoint
            // TODO: send the data to the back-end side => add service for that (consider retry here)
            notSyncedBatches.forEach(batch -> this.gfSyncMetadataStore.saveSyncMetadataOfCalories(batch));
            // TODO: pass metadata to the callback ???
            return null;
        });
    }
}

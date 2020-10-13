package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.core.util.Pair;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private GFClientWrapper client;
    private GFDataUtils gfUtils;
    private GFSyncMetadataStore gfSyncMetadataStore;

    public GoogleFitDataManager(GFClientWrapper client, GFDataUtils gfUtils, GFSyncMetadataStore gfSyncMetadataStore) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
    }

    @SuppressLint("NewApi")
    public void syncCalories(LocalDate start, LocalDate end) {
        // TODO: throw if end is future!
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        client.getCalories(gfQueryDates.first, gfQueryDates.second).continueWith((getCaloriesTask) -> {
            Log.d(TAG, "syncCalories: DONE = " + getCaloriesTask.isSuccessful());
            if (!getCaloriesTask.isSuccessful()) {
                // TODO: invoke callback with exception
//                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                Log.d(TAG, "Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                return null;
            }
            for (GFCalorieDataPoint calorie : getCaloriesTask.getResult()) {
                Log.d(TAG, "Calorie " + calorie);
            }
//            Log.d(TAG, "syncCalories: DONE");
            List<GFCalorieDataPoint> calories = getCaloriesTask.getResult();
            Duration batchDuration = Duration.ofMinutes(30);
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, calories, batchDuration);
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

    public void syncSteps(LocalDate start, LocalDate end) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        // TODO: adjust input dates for steps!
        client.getSteps(gfQueryDates.first, gfQueryDates.second).continueWith((getStepsTask) -> {
            Log.d(TAG, "syncSteps: DONE = " + getStepsTask.isSuccessful());
            if (!getStepsTask.isSuccessful()) {
                // TODO: invoke callback with exception
//                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                Log.d(TAG, "Couldn't get steps from GoogleFit Api: " + getStepsTask.getException().getMessage());
                return null;
            }
            for (GFStepsDataPoint calorie : getStepsTask.getResult()) {
                Log.d(TAG, "Step " + calorie);
            }
           return null;
        });
    }

    public void syncHR(LocalDate start, LocalDate end) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        client.getHRs(gfQueryDates.first, gfQueryDates.second).continueWith((getHRTask) -> {
            Log.d(TAG, "syncHR: DONE = " + getHRTask.isSuccessful());
            if (!getHRTask.isSuccessful()) {
                // TODO: invoke callback with exception
//                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                Log.d(TAG, "Couldn't get HRs from GoogleFit Api: " + getHRTask.getException().getMessage());
                return null;
            }
            Log.d(TAG, "syncHR: TOTAL SIZE: " + getHRTask.getResult().size());
            return null;
        });
    }

    public void syncSessions(LocalDate start, LocalDate end, Duration minimumSessionDuration) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        client.getSessions(gfQueryDates.first, gfQueryDates.second, minimumSessionDuration).continueWith(task -> {
            Log.d(TAG, "syncSessions: DONE = " + task.isSuccessful());
            if (!task.isSuccessful()) {
                // TODO: invoke callback with exception
//                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                Log.d(TAG, "Couldn't get Sessions from GoogleFit Api: " + task.getException().getMessage());
                return null;
            }
            Log.d(TAG, "syncSessions: TOTAL SIZE: " + task.getResult().size());
            for (GFSessionBundle s : task.getResult()) {
                Log.d(TAG, "syncSessions: SESSION " + s);
                for (GFPowerDataPoint power : s.getPower()) {
                    Log.d(TAG, "syncSessions: power " + power);
                }
                for (GFSpeedDataPoint speed : s.getSpeed()) {
                    Log.d(TAG, "syncSessions: speed " + speed);
                }
            }
            return null;
        });
    }
}

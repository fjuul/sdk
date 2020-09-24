package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.entities.IStorage;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class GFSyncMetadataStore {

    private static final float TOTAL_CALORIES_ACCURACY = 0.00001f;

    private IStorage storage;
    private String userToken;
    private SimpleDateFormat dateFormatter;
    private JsonAdapter<GFSyncCaloriesMetadata> syncCaloriesMetadataJsonAdapter;
    private Clock clock;

    @SuppressLint("NewApi")
    public GFSyncMetadataStore(@NonNull IStorage storage, @NonNull String userToken) {
        this(storage, userToken, Clock.systemUTC());
    }

    public GFSyncMetadataStore(@NonNull IStorage storage, @NonNull String userToken, @NonNull Clock clock) {
        this.storage = storage;
        this.userToken = userToken;
        this.clock = clock;
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.syncCaloriesMetadataJsonAdapter = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter())
            .build()
            .adapter(GFSyncCaloriesMetadata.class)
            .nullSafe();
    }

    public boolean isNeededToSyncCaloriesBatch(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final GFSyncCaloriesMetadata storedMetadata = getSyncMetadataOfCalories(caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncCaloriesMetadata newMetadata = buildSyncCaloriesMetadata(caloriesBatch);
        return areSyncCaloriesMetadataDifferent(storedMetadata, newMetadata);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfCalories(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        GFSyncCaloriesMetadata metadata = buildSyncCaloriesMetadata(caloriesBatch);
        String jsonValue = syncCaloriesMetadataJsonAdapter.toJson(metadata);
        String key = buildLookupKey(caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @Nullable
    public GFSyncCaloriesMetadata getSyncMetadataOfCalories(@NonNull Date startTime, @NonNull Date endTime) {
        String key = buildLookupKey(startTime, endTime);
        String jsonValue = storage.get(buildLookupKey(startTime, endTime));
        if (jsonValue == null) {
            return null;
        }
        try {
            return syncCaloriesMetadataJsonAdapter.fromJson(jsonValue);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean areSyncCaloriesMetadataDifferent(GFSyncCaloriesMetadata oldMetadata, GFSyncCaloriesMetadata newMetadata) {
        return oldMetadata.getCount() != newMetadata.getCount() ||
            Math.abs(oldMetadata.getTotalKcals() - newMetadata.getTotalKcals()) >= TOTAL_CALORIES_ACCURACY;
    }

    @SuppressLint("NewApi")
    @NonNull
    private GFSyncCaloriesMetadata buildSyncCaloriesMetadata(GFDataPointsBatch<GFCalorieDataPoint> batch) {
        // TODO: move the sum calculation to batch class (or GFDataUtils)
        float totalKcals = 0;
        for (GFCalorieDataPoint calorie : batch.getPoints()) {
            totalKcals += calorie.getValue();
        }
        int count = batch.getPoints().size();
        Date editedAt = Date.from(clock.instant());
        GFSyncCaloriesMetadata metadata = new GFSyncCaloriesMetadata(count, totalKcals, editedAt);
        return metadata;
    }

    private String buildLookupKey(Date startTime, Date endTime) {
        return String.format("gf-sync-metadata.%s.calories.%s-%s", userToken, dateFormatter.format(startTime), dateFormatter.format(endTime));
    }
}

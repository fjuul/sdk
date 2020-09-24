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
import java.util.Date;
import java.util.TimeZone;

public class GFSyncMetadataStore {
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

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfCalories(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        // TODO: move the sum calculation to batch class (or GFDataUtils)
        float totalKcals = 0;
        for (GFCalorieDataPoint calorie : caloriesBatch.getPoints()) {
            totalKcals += calorie.getValue();
        }
        int count = caloriesBatch.getPoints().size();
        Date editedAt = Date.from(clock.instant());
        GFSyncCaloriesMetadata metadata = new GFSyncCaloriesMetadata(count, totalKcals, editedAt);
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

    private String buildLookupKey(Date startTime, Date endTime) {
        return String.format("gf-sync-metadata.%s.calories.%s-%s", userToken, dateFormatter.format(startTime), dateFormatter.format(endTime));
    }
}

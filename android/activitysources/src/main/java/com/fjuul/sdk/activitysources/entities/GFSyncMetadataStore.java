package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

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
    private JsonAdapter<GFSyncStepsMetadata> syncStepsMetadataJsonAdapter;
    private JsonAdapter<GFSyncHRMetadata> syncHRMetadataJsonAdapter;
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
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        this.syncCaloriesMetadataJsonAdapter = moshi.adapter(GFSyncCaloriesMetadata.class).nullSafe();
        this.syncStepsMetadataJsonAdapter = moshi.adapter(GFSyncStepsMetadata.class).nullSafe();
        this.syncHRMetadataJsonAdapter = moshi.adapter(GFSyncHRMetadata.class).nullSafe();
    }

    // TODO: find a polymorphic way to check/save metadata for all batches
    public boolean isNeededToSyncCaloriesBatch(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final GFSyncCaloriesMetadata storedMetadata = getSyncMetadataOf(GFSyncCaloriesMetadata.class, caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncCaloriesMetadata newMetadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        return newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncStepsBatch(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        final GFSyncStepsMetadata storedMetadata = getSyncMetadataOf(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncStepsMetadata newMetadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);
        return newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncHRBatch(@NonNull GFDataPointsBatch<GFHRDataPoint> hrBatch) {
        final GFSyncHRMetadata storedMetadata = getSyncMetadataOf(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncHRMetadata newMetadata = GFSyncHRMetadata.buildFromBatch(hrBatch, clock);
        return newMetadata.equals(storedMetadata);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfCalories(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        GFSyncCaloriesMetadata metadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        String jsonValue = syncCaloriesMetadataJsonAdapter.toJson(metadata);
        String key = buildLookupKey(GFSyncCaloriesMetadata.class, caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSteps(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        GFSyncStepsMetadata metadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);;
        String jsonValue = syncStepsMetadataJsonAdapter.toJson(metadata);
        String key = buildLookupKey(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfHR(@NonNull GFDataPointsBatch<GFHRDataPoint> hrBatch) {
        GFSyncHRMetadata metadata = GFSyncHRMetadata.buildFromBatch(hrBatch, clock);;
        String jsonValue = syncHRMetadataJsonAdapter.toJson(metadata);
        String key = buildLookupKey(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    private <T extends GFSyncEntityMetadata> T getSyncMetadataOf(Class<T> tClass, @NonNull Date startTime, @NonNull Date endTime) {
        String key = buildLookupKey(tClass, startTime, endTime);
        String jsonValue = storage.get(key);
        if (jsonValue == null) {
            return null;
        }
        JsonAdapter<T> jsonAdapter = getJSONAdapterFor(tClass);
        try {
            return jsonAdapter.fromJson(jsonValue);
        } catch (IOException e) {
            return null;
        }
    }

    private <T extends GFSyncEntityMetadata> String buildLookupKey(Class<T> tClass, Date startTime, Date endTime) {
        String entityKey = null;
        if (tClass == GFSyncStepsMetadata.class) {
            entityKey = "steps";
        } else if (tClass == GFSyncCaloriesMetadata.class) {
            entityKey = "calories";
        } else if (tClass == GFSyncHRMetadata.class) {
            entityKey = "hr";
        } else {
            throw new IllegalArgumentException("Invalid class to evaluate the metadata key");
        }
        return String.format("gf-sync-metadata.%s.%s.%s-%s", userToken, entityKey, dateFormatter.format(startTime), dateFormatter.format(endTime));
    }

    private <T extends GFSyncEntityMetadata> JsonAdapter<T> getJSONAdapterFor(Class<T> tClass) {
        if (tClass == GFSyncStepsMetadata.class) {
            return (JsonAdapter<T>) this.syncStepsMetadataJsonAdapter;
        } else if (tClass == GFSyncCaloriesMetadata.class) {
            return (JsonAdapter<T>) this.syncCaloriesMetadataJsonAdapter;
        } else if (tClass == GFSyncHRMetadata.class) {
            return (JsonAdapter<T>) this.syncHRMetadataJsonAdapter;
        } else {
            throw new IllegalArgumentException("Invalid class to identify the json adapter");
        }
    }
}

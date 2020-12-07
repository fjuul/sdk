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
    private final IStorage storage;
    private final String userToken;
    private final SimpleDateFormat dateFormatter;
    private final JsonAdapter<GFSyncCaloriesMetadata> syncCaloriesMetadataJsonAdapter;
    private final JsonAdapter<GFSyncStepsMetadata> syncStepsMetadataJsonAdapter;
    private final JsonAdapter<GFSyncHRMetadata> syncHRMetadataJsonAdapter;
    private final JsonAdapter<GFSyncSessionMetadata> syncSessionMetadataJsonAdapter;
    private final Clock clock;

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
        this.syncSessionMetadataJsonAdapter = moshi.adapter(GFSyncSessionMetadata.class).nullSafe();
    }

    public boolean isNeededToSyncCaloriesBatch(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final String lookupKey = buildLookupKeyForIntraday(GFSyncCaloriesMetadata.class, caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        final GFSyncCaloriesMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncCaloriesMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncCaloriesMetadata newMetadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncStepsBatch(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        final String lookupKey = buildLookupKeyForIntraday(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        final GFSyncStepsMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncStepsMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncStepsMetadata newMetadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncHRBatch(@NonNull GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch) {
        final String lookupKey = buildLookupKeyForIntraday(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
        final GFSyncHRMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncHRMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncHRMetadata newMetadata = GFSyncHRMetadata.buildFromBatch(hrBatch, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncSessionBundle(@NonNull GFSessionBundle sessionBundle) {
        final String lookupKey = buildLookupKeyForSessionBundle(sessionBundle);
        final GFSyncSessionMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncSessionMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncSessionMetadata newMetadata = GFSyncSessionMetadata.buildFromSessionBundle(sessionBundle, clock);
        return !newMetadata.equals(storedMetadata);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfCalories(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final GFSyncCaloriesMetadata metadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        final String jsonValue = syncCaloriesMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForIntraday(GFSyncCaloriesMetadata.class, caloriesBatch.getStartTime(), caloriesBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSteps(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        final GFSyncStepsMetadata metadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);;
        final String jsonValue = syncStepsMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForIntraday(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfHR(@NonNull GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch) {
        final GFSyncHRMetadata metadata = GFSyncHRMetadata.buildFromBatch(hrBatch, clock);;
        final String jsonValue = syncHRMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForIntraday(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSession(@NonNull GFSessionBundle sessionBundle) {
        final GFSyncSessionMetadata metadata = GFSyncSessionMetadata.buildFromSessionBundle(sessionBundle, clock);;
        final String jsonValue = syncSessionMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForSessionBundle(sessionBundle);
        storage.set(key, jsonValue);
    }

    @Nullable
    private <T extends GFSyncEntityMetadata> T retrieveSyncMetadataOf(Class<T> tClass, @NonNull String lookupKey) {
        final String jsonValue = storage.get(lookupKey);
        if (jsonValue == null) {
            return null;
        }
        final JsonAdapter<T> jsonAdapter = getJSONAdapterFor(tClass);
        try {
            return jsonAdapter.fromJson(jsonValue);
        } catch (IOException e) {
            return null;
        }
    }

    private <T extends GFSyncEntityMetadata> String buildLookupKeyForIntraday(Class<T> tClass, Date startTime, Date endTime) {
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

    private String buildLookupKeyForSessionBundle(GFSessionBundle sessionBundle) {
        return String.format("gf-sync-metadata.%s.sessions.%s", userToken, sessionBundle.getId());
    }

    private <T extends GFSyncEntityMetadata> JsonAdapter<T> getJSONAdapterFor(Class<T> tClass) {
        if (tClass == GFSyncStepsMetadata.class) {
            return (JsonAdapter<T>) this.syncStepsMetadataJsonAdapter;
        } else if (tClass == GFSyncCaloriesMetadata.class) {
            return (JsonAdapter<T>) this.syncCaloriesMetadataJsonAdapter;
        } else if (tClass == GFSyncHRMetadata.class) {
            return (JsonAdapter<T>) this.syncHRMetadataJsonAdapter;
        } else if (tClass == GFSyncSessionMetadata.class) {
            return (JsonAdapter<T>) this.syncSessionMetadataJsonAdapter;
        } else {
            throw new IllegalArgumentException("Invalid class to identify the json adapter");
        }
    }
}

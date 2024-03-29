package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFWeightDataPoint;
import com.fjuul.sdk.core.adapters.LocalDateJsonAdapter;
import com.fjuul.sdk.core.entities.IStorage;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GFSyncMetadataStore {
    private static final String WEIGHT_LOOKUP_KEY = "gf-sync-metadata.weight";
    private static final String HEIGHT_LOOKUP_KEY = "gf-sync-metadata.height";

    private final IStorage storage;
    private final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Nullable
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat format = new SimpleDateFormat("'D'dd'T'HH:mm", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };
    private final ThreadLocal<SimpleDateFormat> sessionListDateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Nullable
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat format = new SimpleDateFormat("'D'dd", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };
    private final JsonAdapter<GFSyncCaloriesMetadata> syncCaloriesMetadataJsonAdapter;
    private final JsonAdapter<GFSyncStepsMetadata> syncStepsMetadataJsonAdapter;
    private final JsonAdapter<GFSyncHRMetadata> syncHRMetadataJsonAdapter;
    private final JsonAdapter<GFSyncSessionMetadata> syncSessionMetadataJsonAdapter;
    private final JsonAdapter<GFSyncSessionsMetadata> syncSessionsMetadataJsonAdapter;
    private final JsonAdapter<GFSyncWeightMetadata> syncWeightMetadataJsonAdapter;
    private final JsonAdapter<GFSyncHeightMetadata> syncHeightMetadataJsonAdapter;
    private final Clock clock;

    @SuppressLint("NewApi")
    public GFSyncMetadataStore(@NonNull IStorage storage) {
        this(storage, Clock.systemUTC());
    }

    @SuppressLint("NewApi")
    public GFSyncMetadataStore(@NonNull IStorage storage, @NonNull Clock clock) {
        this.storage = storage;
        this.clock = clock;
        final Moshi moshi =
            new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).add(new LocalDateJsonAdapter()).build();
        this.syncCaloriesMetadataJsonAdapter = moshi.adapter(GFSyncCaloriesMetadata.class).nullSafe();
        this.syncStepsMetadataJsonAdapter = moshi.adapter(GFSyncStepsMetadata.class).nullSafe();
        this.syncHRMetadataJsonAdapter = moshi.adapter(GFSyncHRMetadata.class).nullSafe();
        this.syncSessionMetadataJsonAdapter = moshi.adapter(GFSyncSessionMetadata.class).nullSafe();
        this.syncSessionsMetadataJsonAdapter = moshi.adapter(GFSyncSessionsMetadata.class).nullSafe();
        this.syncWeightMetadataJsonAdapter = moshi.adapter(GFSyncWeightMetadata.class).nullSafe();
        this.syncHeightMetadataJsonAdapter = moshi.adapter(GFSyncHeightMetadata.class).nullSafe();
    }

    public boolean isNeededToSyncCaloriesBatch(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final String lookupKey = buildLookupKeyForIntraday(GFSyncCaloriesMetadata.class,
            caloriesBatch.getStartTime(),
            caloriesBatch.getEndTime());
        final GFSyncCaloriesMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncCaloriesMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncCaloriesMetadata newMetadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncStepsBatch(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        final String lookupKey =
            buildLookupKeyForIntraday(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        final GFSyncStepsMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncStepsMetadata.class, lookupKey);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncStepsMetadata newMetadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncHRBatch(@NonNull GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch) {
        final String lookupKey =
            buildLookupKeyForIntraday(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
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

    public boolean isNeededToSyncWeight(@NonNull GFWeightDataPoint weightDataPoint) {
        final GFSyncWeightMetadata storedMetadata =
            retrieveSyncMetadataOf(GFSyncWeightMetadata.class, WEIGHT_LOOKUP_KEY);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncWeightMetadata newMetadata = GFSyncWeightMetadata.buildFromDataPoint(weightDataPoint, clock);
        return !newMetadata.equals(storedMetadata);
    }

    public boolean isNeededToSyncHeight(@NonNull GFHeightDataPoint heightDataPoint) {
        final GFSyncHeightMetadata storedMetadata =
            retrieveSyncMetadataOf(GFSyncHeightMetadata.class, HEIGHT_LOOKUP_KEY);
        if (storedMetadata == null) {
            return true;
        }
        final GFSyncHeightMetadata newMetadata = GFSyncHeightMetadata.buildFromDataPoint(heightDataPoint, clock);
        return !newMetadata.equals(storedMetadata);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfCalories(@NonNull GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch) {
        final GFSyncCaloriesMetadata metadata = GFSyncCaloriesMetadata.buildFromBatch(caloriesBatch, clock);
        final String jsonValue = syncCaloriesMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForIntraday(GFSyncCaloriesMetadata.class,
            caloriesBatch.getStartTime(),
            caloriesBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSteps(@NonNull GFDataPointsBatch<GFStepsDataPoint> stepsBatch) {
        final GFSyncStepsMetadata metadata = GFSyncStepsMetadata.buildFromBatch(stepsBatch, clock);;
        final String jsonValue = syncStepsMetadataJsonAdapter.toJson(metadata);
        final String key =
            buildLookupKeyForIntraday(GFSyncStepsMetadata.class, stepsBatch.getStartTime(), stepsBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfHR(@NonNull GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch) {
        final GFSyncHRMetadata metadata = GFSyncHRMetadata.buildFromBatch(hrBatch, clock);;
        final String jsonValue = syncHRMetadataJsonAdapter.toJson(metadata);
        final String key =
            buildLookupKeyForIntraday(GFSyncHRMetadata.class, hrBatch.getStartTime(), hrBatch.getEndTime());
        storage.set(key, jsonValue);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSessions(@NonNull List<GFSessionBundle> sessionBundles) {
        final Map<String, List<GFSessionBundle>> keyToSessionBundles =
            sessionBundles.stream().collect(Collectors.groupingBy(this::buildLookupKeyForSessionBundleList));

        keyToSessionBundles.forEach((key, sessionBundleList) -> {
            final GFSyncSessionsMetadata storedMetadata = retrieveSyncMetadataOf(GFSyncSessionsMetadata.class, key);
            final GFSyncSessionsMetadata newMetadata = GFSyncSessionsMetadata.buildFromList(sessionBundleList, clock);
            if (storedMetadata == null) {
                final String json = syncSessionsMetadataJsonAdapter.toJson(newMetadata);
                storage.set(key, json);
                return;
            }

            if (storedMetadata.getDate().isBefore(newMetadata.getDate())) {
                // remove previously stored stale session metadata
                storedMetadata.getIdentifiers().forEach(sessionId -> {
                    final String staleSessionMetadataKey = buildLookupKeyForSessionBundle(sessionId);
                    storage.set(staleSessionMetadataKey, null);
                });
                final String json = syncSessionsMetadataJsonAdapter.toJson(newMetadata);
                storage.set(key, json);
                return;
            }

            final List<String> mergedIdentifiers =
                Stream.concat(storedMetadata.getIdentifiers().stream(), newMetadata.getIdentifiers().stream())
                    .distinct()
                    .collect(Collectors.toList());
            final GFSyncSessionsMetadata mergedMetadata =
                new GFSyncSessionsMetadata(mergedIdentifiers, newMetadata.getDate(), Date.from(clock.instant()));
            final String json = syncSessionsMetadataJsonAdapter.toJson(mergedMetadata);
            storage.set(key, json);
        });

        sessionBundles.forEach(this::saveSyncMetadataOfSession);
    }

    @SuppressLint("NewApi")
    public void saveSyncMetadataOfSession(@NonNull GFSessionBundle sessionBundle) {
        final GFSyncSessionMetadata metadata = GFSyncSessionMetadata.buildFromSessionBundle(sessionBundle, clock);;
        final String jsonValue = syncSessionMetadataJsonAdapter.toJson(metadata);
        final String key = buildLookupKeyForSessionBundle(sessionBundle);
        storage.set(key, jsonValue);
    }

    public void saveSyncMetadataOfWeight(@NonNull GFWeightDataPoint dataPoint) {
        final GFSyncWeightMetadata metadata = GFSyncWeightMetadata.buildFromDataPoint(dataPoint, clock);
        final String jsonValue = syncWeightMetadataJsonAdapter.toJson(metadata);
        storage.set(WEIGHT_LOOKUP_KEY, jsonValue);
    }

    public void saveSyncMetadataOfHeight(@NonNull GFHeightDataPoint dataPoint) {
        final GFSyncHeightMetadata metadata = GFSyncHeightMetadata.buildFromDataPoint(dataPoint, clock);
        final String jsonValue = syncHeightMetadataJsonAdapter.toJson(metadata);
        storage.set(HEIGHT_LOOKUP_KEY, jsonValue);
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

    private <T extends GFSyncEntityMetadata> String buildLookupKeyForIntraday(Class<T> tClass,
        Date startTime,
        Date endTime) {
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
        return String.format("gf-sync-metadata.%s.%s-%s",
            entityKey,
            dateFormatter.get().format(startTime),
            dateFormatter.get().format(endTime));
    }

    private String buildLookupKeyForSessionBundle(GFSessionBundle sessionBundle) {
        return buildLookupKeyForSessionBundle(sessionBundle.getId());
    }

    private String buildLookupKeyForSessionBundle(String sessionId) {
        return String.format("gf-sync-metadata.session.%s", sessionId);
    }

    private String buildLookupKeyForSessionBundleList(GFSessionBundle sessionBundle) {
        String monthDay = sessionListDateFormatter.get().format(sessionBundle.getTimeStart());
        return String.format("gf-sync-metadata.sessions.%s", monthDay);
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
        } else if (tClass == GFSyncSessionsMetadata.class) {
            return (JsonAdapter<T>) this.syncSessionsMetadataJsonAdapter;
        } else if (tClass == GFSyncHeightMetadata.class) {
            return (JsonAdapter<T>) this.syncHeightMetadataJsonAdapter;
        } else if (tClass == GFSyncWeightMetadata.class) {
            return (JsonAdapter<T>) this.syncWeightMetadataJsonAdapter;
        } else {
            throw new IllegalArgumentException("Invalid class to identify the json adapter");
        }
    }
}

package com.fjuul.sdk.activitysources.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class GFUploadDataJson {
    @NonNull final List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData;
    @NonNull final List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData;
    @NonNull final List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData;
    @NonNull final List<GFSessionJson> sessionsData;

    public GFUploadDataJson(@NonNull List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData,
                            @NonNull List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData,
                            @NonNull List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData,
                            @NonNull List<GFSessionJson> sessionsData) {
        this.caloriesData = caloriesData;
        this.stepsData = stepsData;
        this.hrData = hrData;
        this.sessionsData = sessionsData;
    }

    public static class GFSampleJson<TEntry> {
        @Nullable final String dataSource;
        @NonNull final List<TEntry> entries;

        public GFSampleJson(@Nullable String dataSource, @NonNull List<TEntry> entries) {
            this.dataSource = dataSource;
            this.entries = entries;
        }
    }

    public static class GFSessionJson {
        @NonNull final private String id;
        @Nullable final private String name;
        @Nullable final private String applicationIdentifier;
        @NonNull final private Date timeStart;
        @NonNull final private Date timeEnd;
        final private int type;
        @NonNull final private List<GFSampleJson<GFSampleEntryJson<Integer>>> activitySegments;
        @NonNull final private List<GFSampleJson<GFSampleEntryJson<Float>>> calories;
        @NonNull final private List<GFSampleJson<GFSampleEntryJson<Integer>>> steps;
        @NonNull final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speed;
        @NonNull final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> heartRate;
        @NonNull final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> power;

        public GFSessionJson(@NonNull String id,
                             @Nullable String name,
                             @Nullable String applicationIdentifier,
                             @NonNull Date timeStart,
                             @NonNull Date timeEnd,
                             int type,
                             @NonNull List<GFSampleJson<GFSampleEntryJson<Integer>>> activitySegments,
                             @NonNull List<GFSampleJson<GFSampleEntryJson<Float>>> calories,
                             @NonNull List<GFSampleJson<GFSampleEntryJson<Integer>>> steps,
                             @NonNull List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speed,
                             @NonNull List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> heartRate,
                             @NonNull List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> power) {
            this.id = id;
            this.name = name;
            this.applicationIdentifier = applicationIdentifier;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.type = type;
            this.activitySegments = activitySegments;
            this.calories = calories;
            this.steps = steps;
            this.speed = speed;
            this.heartRate = heartRate;
            this.power = power;
        }
    }

    public static class GFIntradaySampleEntryJson<TValue extends Number> {
        @NonNull final Date start;
        @NonNull final TValue value;

        public GFIntradaySampleEntryJson(@NonNull Date start, @NonNull TValue value) {
            this.start = start;
            this.value = value;
        }
    }

    public static class GFIntradayHRSampleEntryJson {
        @NonNull final Date start;
        final float avg;
        final float min;
        final float max;

        public GFIntradayHRSampleEntryJson(@NonNull Date start, float avg, float min, float max) {
            this.start = start;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }

    public static class GFSampleEntryJson<TValue extends Number> {
        @NonNull final Date start;
        @NonNull final Date end;
        @NonNull final TValue value;

        public GFSampleEntryJson(@NonNull Date start, @NonNull Date end, @NonNull TValue value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    public static class GFInstantMeasureSampleEntryJson<TValue extends Number> {
        @NonNull final Date timestamp;
        @NonNull final TValue value;

        public GFInstantMeasureSampleEntryJson(@NonNull Date timestamp, @NonNull TValue value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}

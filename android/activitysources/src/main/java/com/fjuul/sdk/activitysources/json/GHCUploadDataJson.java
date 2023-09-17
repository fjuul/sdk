package com.fjuul.sdk.activitysources.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class GHCUploadDataJson {
    @NonNull
    final List<GHCSampleJson<GHCIntradaySampleEntryJson<Float>>> caloriesData;
    @NonNull
    final List<GHCSampleJson<GHCIntradaySampleEntryJson<Integer>>> stepsData;
    @NonNull
    final List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRateData;
    @NonNull
    final List<GHCSessionJson> sessionsData;

    public GHCUploadDataJson(@NonNull List<GHCSampleJson<GHCIntradaySampleEntryJson<Float>>> caloriesData,
                             @NonNull List<GHCSampleJson<GHCIntradaySampleEntryJson<Integer>>> stepsData,
                             @NonNull List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRateData,
                             @NonNull List<GHCSessionJson> sessionsData) {
        this.caloriesData = caloriesData;
        this.stepsData = stepsData;
        this.heartRateData = heartRateData;
        this.sessionsData = sessionsData;
    }

    public static class GHCSampleJson<TEntry> {
        @Nullable
        final String dataSource;
        @NonNull
        final List<TEntry> entries;

        public GHCSampleJson(@Nullable String dataSource, @NonNull List<TEntry> entries) {
            this.dataSource = dataSource;
            this.entries = entries;
        }
    }

    public static class GHCSessionJson {
        @NonNull
        final private String id;
        @Nullable
        final private String name;
        @Nullable
        final private String applicationIdentifier;
        @NonNull
        final private Date timeStart;
        @NonNull
        final private Date timeEnd;
        final private int type;
        @NonNull
        final private List<GHCSampleJson<GHCSampleEntryJson<Integer>>> activitySegments;
        @NonNull
        final private List<GHCSampleJson<GHCSampleEntryJson<Float>>> calories;
        @NonNull
        final private List<GHCSampleJson<GHCSampleEntryJson<Integer>>> steps;
        @NonNull
        final private List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRate;
        @NonNull
        final private List<GHCSampleJson<GHCIntradayPowerSampleEntryJson>> power;

        public GHCSessionJson(@NonNull String id,
            @Nullable String name,
            @Nullable String applicationIdentifier,
            @NonNull Date timeStart,
            @NonNull Date timeEnd,
            int type,
            @NonNull List<GHCSampleJson<GHCSampleEntryJson<Integer>>> activitySegments,
            @NonNull List<GHCSampleJson<GHCSampleEntryJson<Float>>> calories,
            @NonNull List<GHCSampleJson<GHCSampleEntryJson<Integer>>> steps,
            @NonNull List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRate,
            @NonNull List<GHCSampleJson<GHCIntradayPowerSampleEntryJson>> power) {
            this.id = id;
            this.name = name;
            this.applicationIdentifier = applicationIdentifier;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.type = type;
            this.activitySegments = activitySegments;
            this.calories = calories;
            this.steps = steps;
            this.heartRate = heartRate;
            this.power = power;
        }
    }

    public static class GHCIntradaySampleEntryJson<TValue extends Number> {
        @NonNull
        final Date start;
        @NonNull
        final TValue value;

        public GHCIntradaySampleEntryJson(@NonNull Date start, @NonNull TValue value) {
            this.start = start;
            this.value = value;
        }
    }

    public static class GHCIntradayHeartRateSampleEntryJson {
        @NonNull
        final Date start;
        final float avg;
        final float min;
        final float max;

        public GHCIntradayHeartRateSampleEntryJson(@NonNull Date start, float avg, float min, float max) {
            this.start = start;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }

    public static class GHCIntradayPowerSampleEntryJson {
        @NonNull
        final Date start;
        final double avg;
        final double min;
        final double max;

        public GHCIntradayPowerSampleEntryJson(@NonNull Date start, double avg, double min, double max) {
            this.start = start;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }

    public static class GHCSampleEntryJson<TValue extends Number> {
        @NonNull
        final Date start;
        @NonNull
        final Date end;
        @NonNull
        final TValue value;

        public GHCSampleEntryJson(@NonNull Date start, @NonNull Date end, @NonNull TValue value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    public static class GHCInstantMeasureSampleEntryJson<TValue extends Number> {
        @NonNull
        final Date timestamp;
        @NonNull
        final TValue value;

        public GHCInstantMeasureSampleEntryJson(@NonNull Date timestamp, @NonNull TValue value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}

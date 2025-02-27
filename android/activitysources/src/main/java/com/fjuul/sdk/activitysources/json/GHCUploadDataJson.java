package com.fjuul.sdk.activitysources.json;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GHCUploadDataJson {
    @NonNull
    final List<GHCSampleJson<GHCIntradaySampleEntryJson<Float>>> caloriesData;
    @NonNull
    final List<GHCSampleJson<GHCIntradaySampleEntryJson<Integer>>> stepsData;
    @NonNull
    final List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRateData;

    public GHCUploadDataJson(@NonNull List<GHCSampleJson<GHCIntradaySampleEntryJson<Float>>> caloriesData,
        @NonNull List<GHCSampleJson<GHCIntradaySampleEntryJson<Integer>>> stepsData,
        @NonNull List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> heartRateData) {
        this.caloriesData = caloriesData;
        this.stepsData = stepsData;
        this.heartRateData = heartRateData;
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

}

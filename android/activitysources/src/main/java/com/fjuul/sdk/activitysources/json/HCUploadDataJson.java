package com.fjuul.sdk.activitysources.json;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCUploadDataJson {
    @NonNull
    final List<HCSampleJson<HCIntradaySampleEntryJson<Float>>> caloriesData;
    @NonNull
    final List<HCSampleJson<HCIntradaySampleEntryJson<Integer>>> stepsData;
    @NonNull
    final List<HCSampleJson<HCIntradayHeartRateSampleEntryJson>> heartRateData;

    public HCUploadDataJson(@NonNull List<HCSampleJson<HCIntradaySampleEntryJson<Float>>> caloriesData,
                            @NonNull List<HCSampleJson<HCIntradaySampleEntryJson<Integer>>> stepsData,
                            @NonNull List<HCSampleJson<HCIntradayHeartRateSampleEntryJson>> heartRateData) {
        this.caloriesData = caloriesData;
        this.stepsData = stepsData;
        this.heartRateData = heartRateData;
    }

    public static class HCSampleJson<TEntry> {
        @Nullable
        final String dataSource;
        @NonNull
        final List<TEntry> entries;

        public HCSampleJson(@Nullable String dataSource, @NonNull List<TEntry> entries) {
            this.dataSource = dataSource;
            this.entries = entries;
        }
    }

    public static class HCIntradaySampleEntryJson<TValue extends Number> {
        @NonNull
        final Date start;
        @NonNull
        final TValue value;

        public HCIntradaySampleEntryJson(@NonNull Date start, @NonNull TValue value) {
            this.start = start;
            this.value = value;
        }
    }

    public static class HCIntradayHeartRateSampleEntryJson {
        @NonNull
        final Date start;
        final float avg;
        final float min;
        final float max;

        public HCIntradayHeartRateSampleEntryJson(@NonNull Date start, float avg, float min, float max) {
            this.start = start;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }

}

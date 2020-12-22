package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GFIntradaySyncOptions extends GFSyncOptions {
    @NonNull private final List<FitnessMetricsType> metrics;

    @NonNull
    public List<FitnessMetricsType> getMetrics() {
        return metrics;
    }

    private GFIntradaySyncOptions(@NonNull List<FitnessMetricsType> metrics, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        super(startDate, endDate);
        this.metrics = metrics;
    }

    public static class Builder {
        @NonNull private Clock clock;
        private Set<FitnessMetricsType> metrics = new HashSet<>();
        @Nullable private LocalDate startDate;
        @Nullable private LocalDate endDate;

        @SuppressLint("NewApi")
        public Builder() {
            this(Clock.systemDefaultZone());
        }

        protected Builder(@NonNull Clock clock) {
            this.clock = clock;
        }

        public Builder include(@NonNull FitnessMetricsType type) {
            if (FitnessMetricsType.INTRADAY_STEPS.equals(type) ||
                FitnessMetricsType.INTRADAY_HEART_RATE.equals(type) ||
                FitnessMetricsType.INTRADAY_CALORIES.equals(type)) {
                metrics.add(type);
            } else {
                throw new IllegalStateException("Not supported fitness metric type for the intraday sync");
            }
            return this;
        }

        public Builder setDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            validateDateInputs(clock, startDate, endDate);
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        @SuppressLint("NewApi")
        public GFIntradaySyncOptions build() {
            if (metrics.isEmpty() || startDate == null || endDate == null) {
                throw new IllegalStateException("Date range and at least one metric type must be specified");
            }
            List<FitnessMetricsType> metricsList = metrics.stream().collect(Collectors.toList());
            return new GFIntradaySyncOptions(metricsList, startDate, endDate);
        }

        @SuppressLint("NewApi")
        @NonNull
        public static LocalDate getMaxAllowedPastDate() {
            return GFSyncOptions.getMaxAllowedPastDate(Clock.systemDefaultZone());
        }
    }
}

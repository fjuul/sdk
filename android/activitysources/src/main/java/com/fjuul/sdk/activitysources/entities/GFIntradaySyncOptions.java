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

        /**
         * Adds the specified intraday fitness metric to the list of data to be synced. This method throws
         * IllegalArgumentException if the fitness metric is not supported for the intraday sync.
         * @param type intraday fitness metric (calories, steps, heart rate)
         * @return builder
         */
        public Builder include(@NonNull FitnessMetricsType type) {
            if (FitnessMetricsType.INTRADAY_STEPS.equals(type) ||
                FitnessMetricsType.INTRADAY_HEART_RATE.equals(type) ||
                FitnessMetricsType.INTRADAY_CALORIES.equals(type)) {
                metrics.add(type);
            } else {
                throw new IllegalArgumentException("Not supported fitness metric type for the intraday sync");
            }
            return this;
        }

        /**
         * Sets start and end dates of intraday data to be synced. This method throws IllegalArgumentException in one of the following cases:
         * <ol>
         *     <li>the start date is after the end date</li>
         *     <li>the end date points to the future</li>
         *     <li>dates exceed the allowed boundary to the past time which is a date of the next day number of the previous
         *     month from today (for example, if today is 20th February, then the max allowed date in the past is 21th January).
         *     In other words, the boundary can be calculated as `today - 1 month + 1 day`. Use {@link GFIntradaySyncOptions.Builder#getMaxAllowedPastDate()} to get the last allowed date of the past for the sync.
         *     </li>
         * </ol>
         * @param startDate start date of intraday data to be synced
         * @param endDate end date of intraday data to be synced
         * @return builder
         */
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

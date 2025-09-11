package com.fjuul.sdk.activitysources.entities;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A class that encapsulates parameters for syncing intraday data of Google Fit. In order to build the instance of this
 * class, use {@link Builder}.
 */
public final class GoogleFitIntradaySyncOptions extends GoogleFitSyncOptions {
    @NonNull
    private final Set<FitnessMetricsType> metrics;

    @NonNull
    public Set<FitnessMetricsType> getMetrics() {
        return metrics;
    }

    private GoogleFitIntradaySyncOptions(@NonNull Set<FitnessMetricsType> metrics,
        @NonNull LocalDate startDate,
        @NonNull LocalDate endDate) {
        super(startDate, endDate);
        this.metrics = metrics;
    }

    /**
     * Builder of {@link GoogleFitIntradaySyncOptions}. The start date, the end date, and at least one fitness metric
     * must be specified during the building.
     */
    public static class Builder {
        @NonNull
        private Clock clock;
        private Set<FitnessMetricsType> metrics = new HashSet<>();
        @Nullable
        private LocalDate startDate;
        @Nullable
        private LocalDate endDate;

        @SuppressLint("NewApi")
        public Builder() {
            this(Clock.systemDefaultZone());
        }

        /**
         * Please use the default constructor (i.e. without any parameters) of Builder because this was added for
         * testing purposes.
         *
         * @param clock system clock
         */
        @VisibleForTesting
        public Builder(@NonNull Clock clock) {
            this.clock = clock;
        }

        /**
         * Adds the specified intraday fitness metric to the set of data to be synced. This method throws
         * IllegalArgumentException if the fitness metric is not supported for the intraday sync.
         *
         * @param type intraday fitness metric (calories, steps, heart rate)
         * @return builder
         */
        @NonNull
        public Builder include(@NonNull FitnessMetricsType type) {
            if (FitnessMetricsType.INTRADAY_STEPS.equals(type)
                || FitnessMetricsType.INTRADAY_HEART_RATE.equals(type)
                || FitnessMetricsType.INTRADAY_CALORIES.equals(type)) {
                metrics.add(type);
            } else {
                throw new IllegalArgumentException("Not supported fitness metric type for the intraday sync");
            }
            return this;
        }

        /**
         * Sets start and end dates of intraday data to be synced. This method throws IllegalArgumentException in one of
         * the following cases:
         * <ol>
         * <li>the start date is after the end date</li>
         * <li>the end date points to the future</li>
         * <li>dates exceed the allowed boundary to the past time which is a date of the next day number of the previous
         * month from today (for example, if today is 20th February, then the max allowed date in the past is 21st
         * January). In other words, the boundary can be calculated as {@code today - 1 month + 1 day}. Use
         * {@link GoogleFitIntradaySyncOptions.Builder#getMaxAllowedPastDate()} to get the last allowed date of the past
         * for the sync.</li>
         * </ol>
         *
         * @param startDate start date of intraday data to be synced
         * @param endDate end date of intraday data to be synced
         * @return builder
         */
        @NonNull
        public Builder setDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            validateDateInputs(clock, startDate, endDate);
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public GoogleFitIntradaySyncOptions build() {
            if (metrics.isEmpty() || startDate == null || endDate == null) {
                throw new IllegalStateException("Date range and at least one metric type must be specified");
            }
            Set<FitnessMetricsType> metricsToSync = metrics.stream().collect(Collectors.toSet());
            return new GoogleFitIntradaySyncOptions(metricsToSync, startDate, endDate);
        }

        @SuppressLint("NewApi")
        @NonNull
        public static LocalDate getMaxAllowedPastDate() {
            return GoogleFitSyncOptions.getMaxAllowedPastDate(Clock.systemDefaultZone());
        }
    }
}

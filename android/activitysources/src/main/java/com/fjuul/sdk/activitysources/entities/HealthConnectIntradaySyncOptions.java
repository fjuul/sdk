package com.fjuul.sdk.activitysources.entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * A class that encapsulates parameters for syncing intraday metrics from Health Connect. In order to
 * build the instance of this class, use {@link Builder}.
 */
public class HealthConnectIntradaySyncOptions {
    @NonNull
    private final Set<FitnessMetricsType> metrics;
    @NonNull
    private final Date startDate;
    @NonNull
    private final Date endDate;

    public HealthConnectIntradaySyncOptions(@NonNull Set<FitnessMetricsType> metrics,
        @NonNull Date startDate, @NonNull Date endDate) {
        this.metrics = metrics;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    public Set<FitnessMetricsType> getMetrics() {
        return metrics;
    }

    @NonNull
    public Date getStartDate() {
        return startDate;
    }

    @NonNull
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Builder of {@link HealthConnectIntradaySyncOptions}. At least one fitness metric must be specified during the
     * building.
     */
    public static class Builder {
        private final Set<FitnessMetricsType> metrics = new HashSet<>();
        private Date startDate;
        private Date endDate;

        /**
         * Adds the given fitness metric to the set of data to be synced. This method throws IllegalArgumentException if
         * the fitness metric is not supported for the intraday sync.
         *
         * @param type fitness metric (calories, steps, heart rate)
         * @return builder
         */
        @NonNull
        public Builder include(@NonNull FitnessMetricsType type) {
            if (FitnessMetricsType.INTRADAY_CALORIES.equals(type) ||
                FitnessMetricsType.INTRADAY_STEPS.equals(type) ||
                FitnessMetricsType.INTRADAY_HEART_RATE.equals(type)) {
                metrics.add(type);
            } else {
                throw new IllegalArgumentException("Not supported fitness metric type for the intraday sync");
            }
            return this;
        }

        /**
         * Sets the date range for syncing data.
         *
         * @param startDate start date
         * @param endDate end date
         * @return builder
         */
        @NonNull
        public Builder setDateRange(@NonNull Date startDate, @NonNull Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public HealthConnectIntradaySyncOptions build() {
            if (metrics.isEmpty()) {
                throw new IllegalStateException("At least one metric type must be specified");
            }
            if (startDate == null || endDate == null) {
                throw new IllegalStateException("Date range must be specified");
            }
            if (startDate.after(endDate)) {
                throw new IllegalStateException("Start date must be before or equal to end date");
            }
            final Set<FitnessMetricsType> metricsToSync = metrics.stream().collect(Collectors.toSet());
            return new HealthConnectIntradaySyncOptions(metricsToSync, startDate, endDate);
        }
    }
} 
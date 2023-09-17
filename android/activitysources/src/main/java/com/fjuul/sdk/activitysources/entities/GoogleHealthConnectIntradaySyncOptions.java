package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that encapsulates parameters for syncing intraday data of Google Health Connect. In
 * order to build the instance of this class, use {@link Builder}.
 */
public final class GoogleHealthConnectIntradaySyncOptions {
    @NonNull
    private final Set<FitnessMetricsType> metrics;

    @NonNull
    public Set<FitnessMetricsType> getMetrics() {
        return metrics;
    }

    private GoogleHealthConnectIntradaySyncOptions(@NonNull Set<FitnessMetricsType> metrics) {
        super();
        this.metrics = metrics;
    }

    /**
     * Builder of {@link GoogleHealthConnectIntradaySyncOptions}. The start date, the end date,
     * and at least one fitness metric must be specified during the building.
     */
    public static class Builder {
        private Set<FitnessMetricsType> metrics = new HashSet<>();

        /**
         * Adds the specified intraday fitness metric to the set of data to be synced. This method
         * throws IllegalArgumentException if the fitness metric is not supported for the intraday
         * sync.
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

        @SuppressLint("NewApi")
        @NonNull
        public GoogleHealthConnectIntradaySyncOptions build() {
            if (metrics.isEmpty()) {
                throw new IllegalStateException("At least one metric type must be specified");
            }
            Set<FitnessMetricsType> metricsToSync = new HashSet<>(metrics);
            return new GoogleHealthConnectIntradaySyncOptions(metricsToSync);
        }
    }
}

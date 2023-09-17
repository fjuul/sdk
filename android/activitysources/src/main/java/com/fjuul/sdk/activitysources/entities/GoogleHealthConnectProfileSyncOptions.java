package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that encapsulates parameters for syncing body parameters of the user profile from Google Fit. In order to
 * build the instance of this class, use {@link Builder}.
 */
public class GoogleHealthConnectProfileSyncOptions {
    @NonNull
    private final Set<FitnessMetricsType> metrics;

    public GoogleHealthConnectProfileSyncOptions(@NonNull Set<FitnessMetricsType> metrics) {
        this.metrics = metrics;
    }

    @NonNull
    public Set<FitnessMetricsType> getMetrics() {
        return metrics;
    }

    /**
     * Builder of {@link GoogleHealthConnectProfileSyncOptions}. At least one fitness metric must be specified during the
     * building.
     */
    public static class Builder {
        private final Set<FitnessMetricsType> metrics = new HashSet<>();

        /**
         * Adds the given user metric to the set of data to be synced. This method throws IllegalArgumentException if
         * the fitness metric is not supported for the profile sync.
         *
         * @param type user metrics (height, weight)
         * @return builder
         */
        @NonNull
        public Builder include(@NonNull FitnessMetricsType type) {
            if (FitnessMetricsType.HEIGHT.equals(type) || FitnessMetricsType.WEIGHT.equals(type)) {
                metrics.add(type);
            } else {
                throw new IllegalArgumentException("Not supported fitness metric type for the profile sync");
            }
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public GoogleHealthConnectProfileSyncOptions build() {
            if (metrics.isEmpty()) {
                throw new IllegalStateException("At least one metric type must be specified");
            }
            final Set<FitnessMetricsType> metricsToSync = new HashSet<>(metrics);
            return new GoogleHealthConnectProfileSyncOptions(metricsToSync);
        }
    }
}

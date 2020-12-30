package com.fjuul.sdk.activitysources.entities;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A configuration class for the ActivitySourceManager that configures its behavior using the specified parameters.
 *
 * @see Builder
 */
public class ActivitySourcesManagerConfig {
    public enum GFBackgroundSyncMode {
        ENABLED,
        DISABLED,
        /**
         * for internal use
         */
        UNTOUCHED
    }

    @NonNull
    private GFBackgroundSyncMode gfIntradayBackgroundSyncMode;
    @NonNull
    private GFBackgroundSyncMode gfSessionsBackgroundSyncMode;
    @Nullable
    private Duration gfSessionsBackgroundSyncMinSessionDuration;

    @NonNull
    private Set<FitnessMetricsType> collectableFitnessMetrics;

    /**
     * Returns the mode that indicates whether intraday data of Google Fit should be synced in the background.
     *
     * @return background mode
     */
    @NonNull
    public GFBackgroundSyncMode getGfIntradayBackgroundSyncMode() {
        return gfIntradayBackgroundSyncMode;
    }

    /**
     * Returns the mode that indicates whether sessions of Google Fit should be synced in the background.
     *
     * @return background mode
     */
    @NonNull
    public GFBackgroundSyncMode getGfSessionsBackgroundSyncMode() {
        return gfSessionsBackgroundSyncMode;
    }

    /**
     * Returns the minimum session duration set for background synchronization.
     *
     * @return duration
     */
    @Nullable
    public Duration getGfSessionsBackgroundSyncMinSessionDuration() {
        return gfSessionsBackgroundSyncMinSessionDuration;
    }

    /**
     * Returns a set of fitness metrics that are collected locally (i.e. not by external trackers).
     *
     * @return set of fitness metrics
     */
    @NonNull
    public Set<FitnessMetricsType> getCollectableFitnessMetrics() {
        return collectableFitnessMetrics;
    }

    public static class Builder {
        @NonNull
        final private ActivitySourcesManagerConfig config;
        private boolean created = false;

        public Builder() {
            config = new ActivitySourcesManagerConfig();
        }

        /**
         * Enables background syncing of intraday data from Google Fit. The types of data to be collected will be
         * determined by the set of collectable fitness metrics. If intraday types are not included in the collectable
         * fitness metrics, the background synchronization will be disabled.
         *
         * @return configured builder
         */
        @SuppressLint("NewApi")
        public Builder enableGFIntradayBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            return this;
        }

        /**
         * Enables background syncing of session data from Google Fit. Sessions with a shorter duration than the
         * specified one will be ignored in the background synchronization. If sessions are not included in the
         * collectable fitness metrics, the background synchronization will be disabled.
         *
         * @param minSessionDuration min duration for sessions to be synced
         * @return configured builder
         */
        public Builder enableGFSessionsBackgroundSync(@NonNull Duration minSessionDuration) {
            Objects.requireNonNull(minSessionDuration, "duration must be not null");
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            config.gfSessionsBackgroundSyncMinSessionDuration = minSessionDuration;
            return this;
        }

        /**
         * Enables background synchronization of intraday and session data from Google Fit.
         *
         * @see #enableGFIntradayBackgroundSync
         * @see #enableGFSessionsBackgroundSync
         * @param minSessionDuration min duration for sessions to be synced
         * @return configured builder
         */
        public Builder enableGFBackgroundSync(@NonNull Duration minSessionDuration) {
            enableGFIntradayBackgroundSync();
            enableGFSessionsBackgroundSync(minSessionDuration);
            return this;
        }

        /**
         * Disables background syncing of intraday data from Google Fit.
         *
         * @return configured builder
         */
        public Builder disableGFIntradayBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.DISABLED;
            return this;
        }

        /**
         * Disables background syncing of session data from Google Fit.
         *
         * @return configured builder
         */
        public Builder disableGFSessionsBackgroundSync() {
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.DISABLED;
            config.gfSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        /**
         * Disables background syncing of intraday and session data from Google Fit.
         *
         * @return configured builder
         */
        public Builder disableGFBackgroundSync() {
            disableGFIntradayBackgroundSync();
            disableGFSessionsBackgroundSync();
            return this;
        }

        /**
         * Sets the special background mode avoiding any changes of scheduled background workers. Currently, it supposed
         * to be used internally only.
         *
         * @return configured builder
         */
        public Builder keepUntouchedGFBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.UNTOUCHED;
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.UNTOUCHED;
            config.gfSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        /**
         * Defines the set of fitness metrics to be collected by local trackers (i.e. Google Fit). The set of metrics is
         * used to determine a list of requested permissions for local activity sources, and to determine background
         * tasks for syncing data from Google Fit. <br>
         * It's allowed to provide an empty set if you intend to use only external activity sources. <br>
         * NOTE: this doesn't affect external trackers and how they request permissions.
         *
         * @param fitnessMetrics set of fitness metrics to be collected by local trackers
         * @return configured builder
         */
        @SuppressLint("NewApi")
        public Builder setCollectableFitnessMetrics(@NonNull Set<FitnessMetricsType> fitnessMetrics) {
            Objects.requireNonNull(fitnessMetrics, "metrics must be not null");
            final Set<FitnessMetricsType> fitnessMetricsReadOnlyCopy =
                Collections.unmodifiableSet(fitnessMetrics.stream().collect(Collectors.toSet()));
            config.collectableFitnessMetrics = fitnessMetricsReadOnlyCopy;
            return this;
        }

        /**
         * Finalizes the configuration and builds the config. This method throws IllegalStateException if it was called
         * more than once.
         *
         * @return config
         */
        public ActivitySourcesManagerConfig build() {
            if (created) {
                throw new IllegalStateException("Do not reuse the builder for creating new instance");
            }
            Objects.requireNonNull(config.gfIntradayBackgroundSyncMode, "GF intraday background sync mode must be set");
            Objects.requireNonNull(config.gfSessionsBackgroundSyncMode, "GF sessions background sync mode must be set");
            Objects.requireNonNull(config.collectableFitnessMetrics, "Collectable fitness metrics must be set");
            this.created = true;
            return config;
        }
    }

    /**
     * Build the default config with all collectable fitness metrics and enabled background synchronization for intraday
     * and session data from Google Fit (minimum duration of sessions is 5 minutes).
     *
     * @return config
     */
    @SuppressLint("NewApi")
    public static ActivitySourcesManagerConfig buildDefault() {
        final Duration minSessionDuration = Duration.ofMinutes(5);
        final Set<FitnessMetricsType> allFitnessMetrics =
            Stream
                .of(FitnessMetricsType.INTRADAY_CALORIES,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    FitnessMetricsType.INTRADAY_STEPS,
                    FitnessMetricsType.WORKOUTS)
                .collect(Collectors.toSet());
        final ActivitySourcesManagerConfig config =
            new ActivitySourcesManagerConfig.Builder().enableGFBackgroundSync(minSessionDuration)
                .setCollectableFitnessMetrics(allFitnessMetrics)
                .build();
        return config;
    }
}

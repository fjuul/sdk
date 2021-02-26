package com.fjuul.sdk.activitysources.entities;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
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
    public enum BackgroundSyncMode {
        ENABLED,
        DISABLED,
        /**
         * for internal use
         */
        UNTOUCHED
    }

    @NonNull
    private BackgroundSyncMode googleFitIntradayBackgroundSyncMode;
    @NonNull
    private BackgroundSyncMode googleFitSessionsBackgroundSyncMode;
    @Nullable
    private Duration googleFitSessionsBackgroundSyncMinSessionDuration;

    @NonNull
    private BackgroundSyncMode profileBackgroundSyncMode;

    @NonNull
    private Set<FitnessMetricsType> collectableFitnessMetrics;

    @Deprecated
    @Nullable
    private Date forcedLowerDateBoundaryForGoogleFit;

    /**
     * Returns the mode that indicates whether intraday data of Google Fit should be synced in the background.
     *
     * @return background mode
     */
    @NonNull
    public BackgroundSyncMode getGoogleFitIntradayBackgroundSyncMode() {
        return googleFitIntradayBackgroundSyncMode;
    }

    /**
     * Returns the mode that indicates whether sessions of Google Fit should be synced in the background.
     *
     * @return background mode
     */
    @NonNull
    public BackgroundSyncMode getGoogleFitSessionsBackgroundSyncMode() {
        return googleFitSessionsBackgroundSyncMode;
    }

    /**
     * Returns the mode that indicates whether user profile properties should be synced from the local activity sources
     * in the background.
     *
     * @return background mode
     */
    @NonNull
    public BackgroundSyncMode getProfileBackgroundSyncMode() {
        return profileBackgroundSyncMode;
    }

    /**
     * Returns the minimum session duration set for background synchronization.
     *
     * @return duration
     */
    @Nullable
    public Duration getGoogleFitSessionsBackgroundSyncMinSessionDuration() {
        return googleFitSessionsBackgroundSyncMinSessionDuration;
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

    @Deprecated
    @Nullable
    public Date getForcedLowerDateBoundaryForGoogleFit() {
        return forcedLowerDateBoundaryForGoogleFit;
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
         * fitness metrics, the background synchronization will be disabled.<br>
         * Note: SDK will schedule background syncs only if there is a current connection to Google Fit. In other words,
         * this option expresses an intent to have the background synchronization when it's applicable but it doesn't
         * mean a requirement of the connection to Google Fit.
         *
         * @return configured builder
         */
        @SuppressLint("NewApi")
        @NonNull
        public Builder enableGoogleFitIntradayBackgroundSync() {
            config.googleFitIntradayBackgroundSyncMode = BackgroundSyncMode.ENABLED;
            return this;
        }

        /**
         * Enables background syncing of session data from Google Fit. Sessions with a shorter duration than the
         * specified one will be ignored in the background synchronization. If sessions are not included in the
         * collectable fitness metrics, the background synchronization will be disabled.<br>
         * Note: SDK will schedule background syncs only if there is a current connection to Google Fit. In other words,
         * this option expresses an intent to have the background synchronization when it's applicable but it doesn't
         * mean a requirement of the connection to Google Fit.
         *
         * @param minSessionDuration min duration for sessions to be synced
         * @return configured builder
         */
        @NonNull
        public Builder enableGoogleFitSessionsBackgroundSync(@NonNull Duration minSessionDuration) {
            Objects.requireNonNull(minSessionDuration, "duration must be not null");
            config.googleFitSessionsBackgroundSyncMode = BackgroundSyncMode.ENABLED;
            config.googleFitSessionsBackgroundSyncMinSessionDuration = minSessionDuration;
            return this;
        }

        /**
         * Enables background synchronization of intraday and session data from Google Fit.<br>
         * Note: SDK will schedule background syncs only if there is a current connection to Google Fit. In other words,
         * this option expresses an intent to have the background synchronization when it's applicable but it doesn't
         * mean a requirement of the connection to Google Fit.
         *
         * @see #enableGoogleFitIntradayBackgroundSync
         * @see #enableGoogleFitSessionsBackgroundSync
         * @param minSessionDuration min duration for sessions to be synced
         * @return configured builder
         */
        @NonNull
        public Builder enableGoogleFitBackgroundSync(@NonNull Duration minSessionDuration) {
            enableGoogleFitIntradayBackgroundSync();
            enableGoogleFitSessionsBackgroundSync(minSessionDuration);
            return this;
        }

        /**
         * Enables background synchronization of user profile properties (e.g. height, weight) from the local activity
         * sources.<br>
         * Note: SDK will schedule background syncs only if there are appropriate current connections to the local
         * activity source. In other words, this option expresses an intent to have the background synchronization when
         * it's applicable but it doesn't mean a strict requirement of having the appropriate connections.<br>
         * If you enabled this then keep in mind that the user profile may be updated anytime, and therefore you should
         * try to get a fresh state of the profile at every session start of your application.
         *
         * @return configured builder
         */
        @NonNull
        public Builder enableProfileBackgroundSync() {
            config.profileBackgroundSyncMode = BackgroundSyncMode.ENABLED;
            return this;
        }

        /**
         * Disables background syncing of intraday data from Google Fit.
         *
         * @return configured builder
         */
        @NonNull
        public Builder disableGoogleFitIntradayBackgroundSync() {
            config.googleFitIntradayBackgroundSyncMode = BackgroundSyncMode.DISABLED;
            return this;
        }

        /**
         * Disables background syncing of session data from Google Fit.
         *
         * @return configured builder
         */
        @NonNull
        public Builder disableGoogleFitSessionsBackgroundSync() {
            config.googleFitSessionsBackgroundSyncMode = BackgroundSyncMode.DISABLED;
            config.googleFitSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        /**
         * Disables background syncing of intraday and session data from Google Fit.
         *
         * @return configured builder
         */
        @NonNull
        public Builder disableGoogleFitBackgroundSync() {
            disableGoogleFitIntradayBackgroundSync();
            disableGoogleFitSessionsBackgroundSync();
            return this;
        }

        /**
         * Disables background syncing of user profile properties (e.g. height, weight) from the local activity sources.
         *
         * @return configured builder
         */
        @NonNull
        public Builder disableProfileBackgroundSync() {
            config.profileBackgroundSyncMode = BackgroundSyncMode.DISABLED;
            return this;
        }

        /**
         * Disables any kind of background syncs. Use this building method if you want to sure that everything must be
         * synchronized only manually by explicit invocations.
         *
         * @return configured builder
         */
        @NonNull
        public Builder disableBackgroundSync() {
            disableGoogleFitBackgroundSync();
            disableProfileBackgroundSync();
            return this;
        }

        /**
         * Sets the special background mode avoiding any changes of scheduled background workers. Currently, it supposed
         * to be used internally only.
         *
         * @return configured builder
         */
        @NonNull
        public Builder keepUntouchedBackgroundSync() {
            config.googleFitIntradayBackgroundSyncMode = BackgroundSyncMode.UNTOUCHED;
            config.googleFitSessionsBackgroundSyncMode = BackgroundSyncMode.UNTOUCHED;
            config.googleFitSessionsBackgroundSyncMinSessionDuration = null;
            config.profileBackgroundSyncMode = BackgroundSyncMode.UNTOUCHED;
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
        @NonNull
        public Builder setCollectableFitnessMetrics(@NonNull Set<FitnessMetricsType> fitnessMetrics) {
            Objects.requireNonNull(fitnessMetrics, "metrics must be not null");
            final Set<FitnessMetricsType> fitnessMetricsReadOnlyCopy =
                Collections.unmodifiableSet(fitnessMetrics.stream().collect(Collectors.toSet()));
            config.collectableFitnessMetrics = fitnessMetricsReadOnlyCopy;
            return this;
        }

        /**
         * Sets the forced lower date boundary from which syncing will work from. It overrides the default limit which
         * is dictated by the creation date of the connection to Google Fit.<br>
         * This method was added only for internal purposes. Do not use it!
         *
         * @param lowerDateBoundary - lower date boundary to sync from
         * @return configured builder
         */
        @Deprecated
        public Builder setForcedLowerDateBoundaryForGoogleFit(@Nullable Date lowerDateBoundary) {
            config.forcedLowerDateBoundaryForGoogleFit = lowerDateBoundary;
            return this;
        }

        /**
         * Finalizes the configuration and builds the config. This method throws IllegalStateException if it was called
         * more than once.
         *
         * @return config
         */
        @NonNull
        public ActivitySourcesManagerConfig build() {
            if (created) {
                throw new IllegalStateException("Do not reuse the builder for creating new instance");
            }
            Objects.requireNonNull(config.googleFitIntradayBackgroundSyncMode,
                "GoogleFit intraday background sync mode must be set");
            Objects.requireNonNull(config.googleFitSessionsBackgroundSyncMode,
                "GoogleFit sessions background sync mode must be set");
            Objects.requireNonNull(config.collectableFitnessMetrics, "Collectable fitness metrics must be set");
            this.created = true;
            return config;
        }
    }

    /**
     * Build the default config with all collectable fitness metrics and enabled background synchronization for intraday
     * and session data from Google Fit (minimum duration of sessions is 5 minutes), user profile properties.
     *
     * @return config
     */
    @SuppressLint("NewApi")
    @NonNull
    public static ActivitySourcesManagerConfig buildDefault() {
        final Duration minSessionDuration = Duration.ofMinutes(5);
        final Set<FitnessMetricsType> allFitnessMetrics =
            Stream
                .of(FitnessMetricsType.INTRADAY_CALORIES,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    FitnessMetricsType.INTRADAY_STEPS,
                    FitnessMetricsType.WORKOUTS,
                    FitnessMetricsType.HEIGHT,
                    FitnessMetricsType.WEIGHT)
                .collect(Collectors.toSet());
        final ActivitySourcesManagerConfig config =
            new ActivitySourcesManagerConfig.Builder().enableGoogleFitBackgroundSync(minSessionDuration)
                .enableProfileBackgroundSync()
                .setCollectableFitnessMetrics(allFitnessMetrics)
                .build();
        return config;
    }
}

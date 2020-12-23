package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActivitySourcesManagerConfig {
    public enum GFBackgroundSyncMode {
        ENABLED,
        DISABLED,
        UNTOUCHED
    }

    @NonNull private GFBackgroundSyncMode gfIntradayBackgroundSyncMode;

    @NonNull private GFBackgroundSyncMode gfSessionsBackgroundSyncMode;
    @Nullable private Duration gfSessionsBackgroundSyncMinSessionDuration;

    @NonNull private Set<FitnessMetricsType> collectableFitnessMetrics;

    @NonNull
    public GFBackgroundSyncMode getGfIntradayBackgroundSyncMode() {
        return gfIntradayBackgroundSyncMode;
    }

    @NonNull
    public GFBackgroundSyncMode getGfSessionsBackgroundSyncMode() {
        return gfSessionsBackgroundSyncMode;
    }

    @Nullable
    public Duration getGfSessionsBackgroundSyncMinSessionDuration() {
        return gfSessionsBackgroundSyncMinSessionDuration;
    }

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

        @SuppressLint("NewApi")
        public Builder enableGFIntradayBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            return this;
        }

        public Builder enableGFSessionsBackgroundSync(@NonNull Duration minSessionDuration) {
            Objects.requireNonNull(minSessionDuration, "duration must be not null");
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            config.gfSessionsBackgroundSyncMinSessionDuration = minSessionDuration;
            return this;
        }

        public Builder enableGFBackgroundSync(@NonNull Duration minSessionDuration) {
            enableGFIntradayBackgroundSync();
            enableGFSessionsBackgroundSync(minSessionDuration);
            return this;
        }

        public Builder disableGFIntradayBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.DISABLED;
            return this;
        }

        public Builder disableGFSessionsBackgroundSync() {
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.DISABLED;
            config.gfSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        public Builder disableGFBackgroundSync() {
            disableGFIntradayBackgroundSync();
            disableGFSessionsBackgroundSync();
            return this;
        }

        public Builder keepUntouchedGFBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.UNTOUCHED;
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.UNTOUCHED;
            config.gfSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        @SuppressLint("NewApi")
        public Builder setCollectableFitnessMetrics(@NonNull Set<FitnessMetricsType> fitnessMetrics) {
            Objects.requireNonNull(fitnessMetrics, "metrics must be not null");
            // TODO: check if the set is empty ?
            final Set<FitnessMetricsType> fitnessMetricsReadOnlyCopy = Collections.unmodifiableSet(
                fitnessMetrics.stream().collect(Collectors.toSet()));
            config.collectableFitnessMetrics = fitnessMetricsReadOnlyCopy;
            return this;
        }

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

    @SuppressLint("NewApi")
    public static ActivitySourcesManagerConfig buildDefault() {
        final Duration minSessionDuration = Duration.ofMinutes(5);
        final Set<FitnessMetricsType> allFitnessMetrics = Stream.of(
            FitnessMetricsType.INTRADAY_CALORIES,
            FitnessMetricsType.INTRADAY_HEART_RATE,
            FitnessMetricsType.INTRADAY_STEPS,
            FitnessMetricsType.WORKOUTS
        ).collect(Collectors.toSet());
        final ActivitySourcesManagerConfig config = new ActivitySourcesManagerConfig.Builder()
            .enableGFBackgroundSync(minSessionDuration)
            .setCollectableFitnessMetrics(allFitnessMetrics)
            .build();
        return config;
    }
}

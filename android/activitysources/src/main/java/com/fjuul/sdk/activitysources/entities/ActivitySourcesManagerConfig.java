package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActivitySourcesManagerConfig {
    enum GFBackgroundSyncMode {
        ENABLED,
        DISABLED,
        UNTOUCHED
    }

    @NonNull private GFBackgroundSyncMode gfIntradayBackgroundSyncMode;
    @Nullable private List<GFIntradaySyncOptions.METRICS_TYPE> gfIntradayBackgroundSyncMetrics;

    @NonNull private GFBackgroundSyncMode gfSessionsBackgroundSyncMode;
    @Nullable private Duration gfSessionsBackgroundSyncMinSessionDuration;

    @NonNull
    public GFBackgroundSyncMode getGfIntradayBackgroundSyncMode() {
        return gfIntradayBackgroundSyncMode;
    }

    @Nullable
    public List<GFIntradaySyncOptions.METRICS_TYPE> getGfIntradayBackgroundSyncMetrics() {
        return gfIntradayBackgroundSyncMetrics;
    }

    @NonNull
    public GFBackgroundSyncMode getGfSessionsBackgroundSyncMode() {
        return gfSessionsBackgroundSyncMode;
    }

    @Nullable
    public Duration getGfSessionsBackgroundSyncMinSessionDuration() {
        return gfSessionsBackgroundSyncMinSessionDuration;
    }

    public static class Builder {
        @NonNull
        final private ActivitySourcesManagerConfig config;
        private boolean created = false;

        public Builder() {
            config = new ActivitySourcesManagerConfig();
        }

        @SuppressLint("NewApi")
        public Builder enableGFIntradayBackgroundSync(@NonNull Set<GFIntradaySyncOptions.METRICS_TYPE> intradayMetrics) {
            Objects.requireNonNull(intradayMetrics, "intraday metrics must be not null");
            if (intradayMetrics.isEmpty()) {
                throw new IllegalArgumentException("Intraday metrics must be not empty");
            }
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            config.gfIntradayBackgroundSyncMetrics = intradayMetrics.stream().collect(Collectors.toList());
            return this;
        }

        public Builder enableGFSessionsBackgroundSync(@NonNull Duration minSessionDuration) {
            Objects.requireNonNull(minSessionDuration, "duration must be not null");
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.ENABLED;
            config.gfSessionsBackgroundSyncMinSessionDuration = minSessionDuration;
            return this;
        }

        public Builder enableGFBackgroundSync(@NonNull Set<GFIntradaySyncOptions.METRICS_TYPE> intradayMetrics, @NonNull Duration minSessionDuration) {
            enableGFIntradayBackgroundSync(intradayMetrics);
            enableGFSessionsBackgroundSync(minSessionDuration);
            return this;
        }

        public Builder disableGFIntradayBackgroundSync() {
            config.gfIntradayBackgroundSyncMode = GFBackgroundSyncMode.DISABLED;
            config.gfIntradayBackgroundSyncMetrics = null;
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
            config.gfIntradayBackgroundSyncMetrics = null;
            config.gfSessionsBackgroundSyncMode = GFBackgroundSyncMode.UNTOUCHED;
            config.gfSessionsBackgroundSyncMinSessionDuration = null;
            return this;
        }

        public ActivitySourcesManagerConfig build() {
            if (created) {
                throw new IllegalStateException("Do not reuse the builder for creating new instance");
            }
            Objects.requireNonNull(config.gfIntradayBackgroundSyncMode, "GF intraday background sync mode must be set");
            Objects.requireNonNull(config.gfSessionsBackgroundSyncMode, "GF sessions background sync mode must be set");
            this.created = true;
            return config;
        }
    }

    // todo: how to disable the background worker if a user did logout and there is not known credentials

    @SuppressLint("NewApi")
    public static ActivitySourcesManagerConfig buildDefault() {
        final Set<GFIntradaySyncOptions.METRICS_TYPE> intradayMetrics = Stream.of(
            GFIntradaySyncOptions.METRICS_TYPE.CALORIES,
            GFIntradaySyncOptions.METRICS_TYPE.HEART_RATE,
            GFIntradaySyncOptions.METRICS_TYPE.STEPS
        ).collect(Collectors.toSet());
        final Duration minSessionDuration = Duration.ofMinutes(5);
        final ActivitySourcesManagerConfig config = new ActivitySourcesManagerConfig.Builder()
            .enableGFBackgroundSync(intradayMetrics, minSessionDuration)
            .build();
        return config;
    }
}
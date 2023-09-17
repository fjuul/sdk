package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

/**
 * A class that encapsulates parameters for syncing sessions of Google Health Connect. In order to
 * build the instance of this class, use {@link Builder}.
 */
public final class GoogleHealthConnectSessionSyncOptions {
    @NonNull
    private final Duration minimumSessionDuration;

    private GoogleHealthConnectSessionSyncOptions(@NonNull Duration minimumSessionDuration) {
        this.minimumSessionDuration = minimumSessionDuration;
    }

    @NonNull
    public Duration getMinimumSessionDuration() {
        return minimumSessionDuration;
    }

    /**
     * Builder of {@link GoogleHealthConnectSessionSyncOptions}. The minimum session duration
     * must be specified during the building.
     */
    public static class Builder {
        @Nullable
        private Duration minimumSessionDuration;

        /**
         * Sets the minimum session duration to be taken into account when querying sessions during the synchronization.
         * Sessions with a shorter duration than the specified one will be ignored.<br>
         * Note: the duration that is too short may take a longer amount of synchronization time if a user has a lot of
         * small sessions.
         *
         * @param duration min duration for sessions to be synced
         * @return
         */
        @NonNull
        public Builder setMinimumSessionDuration(@NonNull Duration duration) {
            this.minimumSessionDuration = duration;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public GoogleHealthConnectSessionSyncOptions build() {
            if (minimumSessionDuration == null) {
                throw new IllegalStateException("Minimum session duration must be specified");
            }
            return new GoogleHealthConnectSessionSyncOptions(minimumSessionDuration);
        }
    }
}

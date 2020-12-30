package com.fjuul.sdk.activitysources.entities;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A class that encapsulates parameters for syncing sessions of Google Fit. In order to build the instance of this
 * class, use {@link Builder}.
 */
public final class GFSessionSyncOptions extends GFSyncOptions {
    @NonNull
    private final Duration minimumSessionDuration;

    private GFSessionSyncOptions(@NonNull LocalDate startDate,
        @NonNull LocalDate endDate,
        @NonNull Duration minimumSessionDuration) {
        super(startDate, endDate);
        this.minimumSessionDuration = minimumSessionDuration;
    }

    @NonNull
    public Duration getMinimumSessionDuration() {
        return minimumSessionDuration;
    }

    /**
     * Builder of {@link GFSessionSyncOptions}. The start date, the end date, and the minimum session duration must be
     * specified during the building.
     */
    public static class Builder {
        @NonNull
        private final Clock clock;
        @Nullable
        private LocalDate startDate;
        @Nullable
        private LocalDate endDate;
        @Nullable
        private Duration minimumSessionDuration;

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
         * Sets start and end dates of sessions to be synced. This method throws IllegalArgumentException in one of the
         * following cases:
         * <ol>
         * <li>the start date is after the end date</li>
         * <li>the end date points to the future</li>
         * <li>dates exceed the allowed boundary to the past time which is a date of the next day number of the previous
         * month from today (for example, if today is 20th February, then the max allowed date in the past is 21th
         * January). In other words, the boundary can be calculated as {@code today - 1 month + 1 day}. Use
         * {@link Builder#getMaxAllowedPastDate()} to get the last allowed date of the past for the sync.</li>
         * </ol>
         *
         * @param startDate start date of sessions to be synced
         * @param endDate end date of sessions to be synced
         * @return builder
         */
        @NonNull
        public Builder setDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            validateDateInputs(clock, startDate, endDate);
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

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
        public GFSessionSyncOptions build() {
            if (startDate == null || endDate == null || minimumSessionDuration == null) {
                throw new IllegalStateException("Date range and minimum session duration must be specified");
            }
            return new GFSessionSyncOptions(startDate, endDate, minimumSessionDuration);
        }

        @SuppressLint("NewApi")
        @NonNull
        public static LocalDate getMaxAllowedPastDate() {
            return GFSyncOptions.getMaxAllowedPastDate(Clock.systemDefaultZone());
        }
    }
}

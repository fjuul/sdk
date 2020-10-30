package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;

public final class GFSessionSyncOptions {
    @NonNull final LocalDate startDate;
    @NonNull final LocalDate endDate;
    @NonNull final Duration minimumSessionDuration;

    private GFSessionSyncOptions(@NonNull LocalDate startDate, @NonNull LocalDate endDate, @NonNull Duration minimumSessionDuration) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.minimumSessionDuration = minimumSessionDuration;
    }

    public static class Builder {
        @Nullable LocalDate startDate;
        @Nullable LocalDate endDate;
        @Nullable Duration minimumSessionDuration;

        public Builder setDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        public Builder setMinimumSessionDuration(@NonNull Duration duration) {
            this.minimumSessionDuration = duration;
            return this;
        }

        @SuppressLint("NewApi")
        public GFSessionSyncOptions build() {
            if (startDate == null || endDate == null || minimumSessionDuration == null) {
                throw new IllegalStateException("Date range and minimum session duration must be specified");
            }
            return new GFSessionSyncOptions(startDate, endDate, minimumSessionDuration);
        }
    }
}

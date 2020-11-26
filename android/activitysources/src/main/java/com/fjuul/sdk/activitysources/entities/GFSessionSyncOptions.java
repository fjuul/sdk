package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;

public final class GFSessionSyncOptions {
    @NonNull private final LocalDate startDate;
    @NonNull private final LocalDate endDate;
    @NonNull private final Duration minimumSessionDuration;

    private GFSessionSyncOptions(@NonNull LocalDate startDate, @NonNull LocalDate endDate, @NonNull Duration minimumSessionDuration) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.minimumSessionDuration = minimumSessionDuration;
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    @NonNull
    public LocalDate getEndDate() {
        return endDate;
    }

    @NonNull
    public Duration getMinimumSessionDuration() {
        return minimumSessionDuration;
    }

    public static class Builder {
        @Nullable private LocalDate startDate;
        @Nullable private LocalDate endDate;
        @Nullable private Duration minimumSessionDuration;

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

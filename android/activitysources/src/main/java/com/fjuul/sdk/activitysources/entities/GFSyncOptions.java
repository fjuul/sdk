package com.fjuul.sdk.activitysources.entities;

import java.time.Clock;
import java.time.LocalDate;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public abstract class GFSyncOptions {
    @NonNull
    protected final LocalDate startDate;
    @NonNull
    protected final LocalDate endDate;

    public GFSyncOptions(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    @NonNull
    public LocalDate getEndDate() {
        return endDate;
    }

    @SuppressLint("NewApi")
    protected static void validateDateInputs(@NonNull Clock clock,
        @NonNull LocalDate startDate,
        @NonNull LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("The start date must be less or equal to the end date");
        }
        if (endDate.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("The end date must not point at the future");
        }
        final LocalDate borderDate = LocalDate.now(clock).minusMonths(1).plusDays(1);
        if (startDate.isBefore(borderDate) || endDate.isBefore(borderDate)) {
            throw new IllegalArgumentException("Input dates must not exceed the max allowed border");
        }
    }

    @SuppressLint("NewApi")
    @NonNull
    protected static LocalDate getMaxAllowedPastDate(@NonNull Clock clock) {
        return LocalDate.now(clock).minusMonths(1).plusDays(1);
    }
}

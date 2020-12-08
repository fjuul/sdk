package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class GFSyncStepsMetadata extends GFSyncDatedEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private int count;
    private int totalSteps;

    public GFSyncStepsMetadata(int count, int totalSteps, LocalDate date, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, date, editedAt);
        this.count = count;
        this.totalSteps = totalSteps;
    }

    @NonNull
    public int getCount() {
        return count;
    }

    @NonNull
    public int getTotalSteps() {
        return totalSteps;
    }

    @SuppressLint("NewApi")
    static public GFSyncStepsMetadata buildFromBatch(GFDataPointsBatch<GFStepsDataPoint> batch, Clock clock) {
        // TODO: move the sum calculation to batch class (or GFDataUtils)
        int totalSteps = batch.getPoints().stream().mapToInt(s -> s.getValue()).sum();
        int count = batch.getPoints().size();
        final Date editedAt = Date.from(clock.instant());
        final LocalDate date = batch.getStartTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        return new GFSyncStepsMetadata(count, totalSteps, date, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFSyncStepsMetadata that = (GFSyncStepsMetadata) o;
        return count == that.count && totalSteps == that.totalSteps;
    }
}

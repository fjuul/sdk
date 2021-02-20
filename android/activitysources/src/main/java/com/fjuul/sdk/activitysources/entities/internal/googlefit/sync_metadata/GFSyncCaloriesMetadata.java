package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public final class GFSyncCaloriesMetadata extends GFSyncDatedEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final float TOTAL_CALORIES_ACCURACY = 0.001f;
    private int count;
    private float totalKcals;

    public GFSyncCaloriesMetadata(int count, float totalKcals, @NonNull LocalDate date, @NonNull Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, date, editedAt);
        this.count = count;
        this.totalKcals = totalKcals;
    }

    @NonNull
    public int getCount() {
        return count;
    }

    @NonNull
    public float getTotalKcals() {
        return totalKcals;
    }

    @SuppressLint("NewApi")
    @NonNull
    static public GFSyncCaloriesMetadata buildFromBatch(@NonNull GFDataPointsBatch<GFCalorieDataPoint> batch,
        @NonNull Clock clock) {
        float totalKcals = batch.getPoints().stream().map(c -> c.getValue()).reduce(0f, (acc, el) -> acc + el);
        int count = batch.getPoints().size();
        final Date editedAt = Date.from(clock.instant());
        final LocalDate date = batch.getStartTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        return new GFSyncCaloriesMetadata(count, totalKcals, date, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFSyncCaloriesMetadata that = (GFSyncCaloriesMetadata) o;
        return Objects.equals(date, that.date)
            && count == that.count
            && Math.abs(totalKcals - that.totalKcals) <= TOTAL_CALORIES_ACCURACY;
    }
}

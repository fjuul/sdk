package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class GFSyncHRMetadata extends GFSyncDatedEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final float TOTAL_HR_ACCURACY = 0.01f;
    private int count;
    private float sumOfAverages;

    public GFSyncHRMetadata(int count, float sumOfAverages, LocalDate date, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, date, editedAt);
        this.count = count;
        this.sumOfAverages = sumOfAverages;
    }

    @NonNull
    public int getCount() {
        return count;
    }

    @NonNull
    public float getSumOfAverages() {
        return sumOfAverages;
    }

    @SuppressLint("NewApi")
    static public GFSyncHRMetadata buildFromBatch(GFDataPointsBatch<GFHRSummaryDataPoint> batch, Clock clock) {
        float totalSum = batch.getPoints().stream().map(hr -> hr.getAvg()).reduce(0f, (acc, el) -> acc + el);
        int count = batch.getPoints().size();
        final Date editedAt = Date.from(clock.instant());
        final LocalDate date = batch.getStartTime().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        return new GFSyncHRMetadata(count, totalSum, date, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFSyncHRMetadata that = (GFSyncHRMetadata) o;
        return Objects.equals(date, that.date) && count == that.count
            && Math.abs(sumOfAverages - that.sumOfAverages) <= TOTAL_HR_ACCURACY;
    }
}

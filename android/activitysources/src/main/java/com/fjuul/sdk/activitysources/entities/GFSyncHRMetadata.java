package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.time.Clock;
import java.util.Date;

public class GFSyncHRMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final float TOTAL_HR_ACCURACY = 0.01f;
    private int count;
    private float totalSum;

    public GFSyncHRMetadata(int count, float totalSum, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
        this.count = count;
        this.totalSum = totalSum;
    }

    @NonNull
    public int getCount() {
        return count;
    }

    @NonNull
    public float getTotalSum() {
        return totalSum;
    }

    @SuppressLint("NewApi")
    static public GFSyncHRMetadata buildFromBatch(GFDataPointsBatch<GFHRDataPoint> batch, Clock clock) {
        float totalSum = batch.getPoints().stream()
            .map(hr -> hr.getValue())
            .reduce(0f, (acc, el) -> acc + el);
        int count = batch.getPoints().size();
        Date editedAt = Date.from(clock.instant());
        return new GFSyncHRMetadata (count, totalSum, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFSyncHRMetadata that = (GFSyncHRMetadata) o;
        return count == that.count && Math.abs(totalSum - that.totalSum) <= TOTAL_HR_ACCURACY;
    }
}
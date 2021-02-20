package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.Clock;
import java.util.Date;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFWeightDataPoint;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class GFSyncWeightMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final float WEIGHT_KG_ACCURACY = 0.01f;

    private final float weight;
    @NonNull
    private final Date createdAt;

    public GFSyncWeightMetadata(float weight, @NonNull Date createdAt, @NonNull Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
        this.weight = weight;
        this.createdAt = createdAt;
    }

    @SuppressLint("NewApi")
    static public GFSyncWeightMetadata buildFromDataPoint(@NonNull GFWeightDataPoint dataPoint, @NonNull Clock clock) {
        float weight = dataPoint.getValue();
        final Date date = dataPoint.getStart();
        final Date editedAt = Date.from(clock.instant());
        return new GFSyncWeightMetadata(weight, date, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GFSyncWeightMetadata that = (GFSyncWeightMetadata) o;
        return createdAt.equals(that.createdAt) && Math.abs(weight - that.weight) <= WEIGHT_KG_ACCURACY;
    }
}

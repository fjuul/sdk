package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.Clock;
import java.util.Date;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHeightDataPoint;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class GFSyncHeightMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final float HEIGHT_CM_ACCURACY = 0.1f;

    private final float height;
    @NonNull
    private final Date createdAt;

    public GFSyncHeightMetadata(float height, @NonNull Date createdAt, @NonNull Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
        this.height = height;
        this.createdAt = createdAt;
    }

    @SuppressLint("NewApi")
    @NonNull
    static public GFSyncHeightMetadata buildFromDataPoint(@NonNull GFHeightDataPoint dataPoint, @NonNull Clock clock) {
        float height = dataPoint.getValue();
        final Date date = dataPoint.getStart();
        final Date editedAt = Date.from(clock.instant());
        return new GFSyncHeightMetadata(height, date, editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GFSyncHeightMetadata that = (GFSyncHeightMetadata) o;
        return createdAt.equals(that.createdAt) && Math.abs(height - that.height) <= HEIGHT_CM_ACCURACY;
    }
}

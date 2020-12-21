package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;

import java.time.Clock;
import java.util.Date;
import java.util.Objects;

class GFSyncSessionMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @NonNull private final String id;
    @Nullable private final String name;
    @Nullable private final String applicationIdentifier;
    @NonNull private final Date timeStart;
    @NonNull private final Date timeEnd;
    private final int type;
    private final int activitySegmentsCount;
    private final int caloriesCount;
    private final int stepsCount;
    private final int heartRateCount;
    private final int speedCount;
    private final int powerCount;

    public GFSyncSessionMetadata(@NonNull String id, @Nullable String name, @Nullable String applicationIdentifier,
                                 @NonNull Date timeStart, @NonNull Date timeEnd, int type,
                                 int activitySegmentsCount, int caloriesCount, int stepsCount,
                                 int heartRateCount, int speedCount, int powerCount, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
        this.id = id;
        this.name = name;
        this.applicationIdentifier = applicationIdentifier;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.type = type;
        this.activitySegmentsCount = activitySegmentsCount;
        this.caloriesCount = caloriesCount;
        this.stepsCount = stepsCount;
        this.heartRateCount = heartRateCount;
        this.speedCount = speedCount;
        this.powerCount = powerCount;
    }

    @SuppressLint("NewApi")
    static public GFSyncSessionMetadata buildFromSessionBundle(@NonNull GFSessionBundle sessionBundle, @NonNull Clock clock) {
        final int activitySegmentsCount = sessionBundle.getActivitySegments().size();
        final int caloriesCount = sessionBundle.getCalories().size();
        final int stepsCount = sessionBundle.getSteps().size();
        final int heartRateCount = sessionBundle.getHeartRate().size();
        final int speedCount = sessionBundle.getSpeed().size();
        final int powerCount = sessionBundle.getPower().size();
        final Date editedAt = Date.from(clock.instant());
        return new GFSyncSessionMetadata(
            sessionBundle.getId(),
            sessionBundle.getName(),
            sessionBundle.getApplicationIdentifier(),
            sessionBundle.getTimeStart(),
            sessionBundle.getTimeEnd(),
            sessionBundle.getType(),
            activitySegmentsCount,
            caloriesCount,
            stepsCount,
            heartRateCount,
            speedCount,
            powerCount,
            editedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFSyncSessionMetadata that = (GFSyncSessionMetadata) o;
        return type == that.type &&
            activitySegmentsCount == that.activitySegmentsCount &&
            caloriesCount == that.caloriesCount &&
            stepsCount == that.stepsCount &&
            heartRateCount == that.heartRateCount &&
            speedCount == that.speedCount &&
            powerCount == that.powerCount &&
            id.equals(that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(applicationIdentifier, that.applicationIdentifier) &&
            timeStart.equals(that.timeStart) &&
            timeEnd.equals(that.timeEnd);
    }
}

package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public final class GFSyncCaloriesMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private int count;
    private float totalKcals;

    public GFSyncCaloriesMetadata(int count, float totalKcals, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
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
}

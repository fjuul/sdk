package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public class GFSyncCaloriesMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion;
    private int count;
    private float totalKcals;
    private Date editedAt;

    public GFSyncCaloriesMetadata(int count, float totalKcals, Date editedAt) {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        this.count = count;
        this.totalKcals = totalKcals;
        this.editedAt = editedAt;
    }

    @NonNull
    public int getCount() {
        return count;
    }

    @NonNull
    public float getTotalKcals() {
        return totalKcals;
    }

    @NonNull
    public Date getEditedAt() {
        return editedAt;
    }

    @NonNull
    public int getSchemaVersion() {
        return schemaVersion;
    }
}

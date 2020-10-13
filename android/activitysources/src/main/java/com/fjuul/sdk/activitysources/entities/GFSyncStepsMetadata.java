package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public class GFSyncStepsMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private int count;
    private int totalSteps;

    public GFSyncStepsMetadata(int count, int totalSteps, Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, editedAt);
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
}

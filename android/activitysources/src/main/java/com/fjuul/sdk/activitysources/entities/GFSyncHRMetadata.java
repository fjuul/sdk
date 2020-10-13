package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public class GFSyncHRMetadata extends GFSyncEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private int count;
    private float totalSum;

    public GFSyncHRMetadata(int count, float, Date editedAt) {
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
}

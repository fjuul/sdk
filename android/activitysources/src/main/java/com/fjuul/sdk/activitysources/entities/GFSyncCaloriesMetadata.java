package com.fjuul.sdk.activitysources.entities;

import java.util.Date;

public class GFSyncCaloriesMetadata {
    private int count;
    private float totalKcals;
    private Date editedAt;

    public GFSyncCaloriesMetadata(int count, float totalKcals, Date editedAt) {
        this.count = count;
        this.totalKcals = totalKcals;
        this.editedAt = editedAt;
    }

    public int getCount() {
        return count;
    }

    public float getTotalKcals() {
        return totalKcals;
    }

    public Date getEditedAt() {
        return editedAt;
    }
}

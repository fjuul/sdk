package com.fjuul.sdk.activitysources.entities;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TrackerConnection {
    @NonNull
    private String id;

    @NonNull
    private String tracker;

    @NonNull
    private Date createdAt;

    @Nullable
    private Date endedAt;

    public TrackerConnection(@NonNull String id, @NonNull String tracker, @NonNull Date createdAt,
        @NonNull Date endedAt) {
        this.id = id;
        this.tracker = tracker;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTracker() {
        return tracker;
    }

    @NonNull
    public Date getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Date getEndedAt() {
        return endedAt;
    }
}

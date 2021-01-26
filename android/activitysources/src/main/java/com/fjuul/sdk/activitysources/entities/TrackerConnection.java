package com.fjuul.sdk.activitysources.entities;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class that represents information about the connected activity source.
 */
public class TrackerConnection {
    @NonNull
    private String id;

    @NonNull
    private String tracker;

    @NonNull
    private Date createdAt;

    @Nullable
    private Date endedAt;

    public TrackerConnection(@NonNull String id,
        @NonNull String tracker,
        @NonNull Date createdAt,
        @NonNull Date endedAt) {
        this.id = id;
        this.tracker = tracker;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }

    /**
     * Returns the unique ID of the established connection.
     *
     * @return id
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Returns the underlying string presentation of the connected activity source. This value can be matched with
     * static constants of {@link TrackerValue} by {@link TrackerValue#getValue()}.
     *
     * @return tracker value
     */
    @NonNull
    public String getTracker() {
        return tracker;
    }

    /**
     * Returns the date when the connection was created.
     *
     * @return creation date
     */
    @NonNull
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the end date of the established connection. If it's null, then this tracker connection is still active.
     *
     * @return ending date
     */
    @Nullable
    public Date getEndedAt() {
        return endedAt;
    }
}

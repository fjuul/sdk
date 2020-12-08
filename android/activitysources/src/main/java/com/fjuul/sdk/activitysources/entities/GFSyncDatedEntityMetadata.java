package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.Date;

public abstract class GFSyncDatedEntityMetadata extends GFSyncEntityMetadata {
    @NonNull
    protected final LocalDate date;

    public GFSyncDatedEntityMetadata(int schemaVersion, @NonNull LocalDate date, @NonNull Date editedAt) {
        super(schemaVersion, editedAt);
        this.date = date;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }
}

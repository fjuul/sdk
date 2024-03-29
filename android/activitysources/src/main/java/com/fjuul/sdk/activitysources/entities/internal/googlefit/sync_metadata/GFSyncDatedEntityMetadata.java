package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.LocalDate;
import java.util.Date;

import androidx.annotation.NonNull;

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

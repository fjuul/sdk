package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public abstract class GFSyncEntityMetadata {
    @NonNull protected final int schemaVersion;
    @NonNull protected final Date editedAt;

    public GFSyncEntityMetadata(int schemaVersion, @NonNull Date editedAt) {
        this.schemaVersion = schemaVersion;
        this.editedAt = editedAt;
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

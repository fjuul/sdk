package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public abstract class GFSyncEntityMetadata {
    protected int schemaVersion;
    protected Date editedAt;

    public GFSyncEntityMetadata(int schemaVersion, Date editedAt) {
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

package com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata;

import java.util.Date;
import java.util.Optional;

import com.fjuul.sdk.core.storage.Storage;

public class HCSyncMetadataStore {
    private static final String LAST_SYNC_TIME_KEY = "health_connect_last_sync_time";
    private static final String LOWER_DATE_BOUNDARY_KEY = "health_connect_lower_date_boundary";

    private final Storage storage;

    public HCSyncMetadataStore(@NonNull Storage storage) {
        this.storage = storage;
    }

    public void setLastSyncTime(@NonNull Date date) {
        storage.put(LAST_SYNC_TIME_KEY, date.getTime());
    }

    @NonNull
    public Optional<Date> getLastSyncTime() {
        Long timestamp = storage.getLong(LAST_SYNC_TIME_KEY);
        if (timestamp == null) {
            return Optional.empty();
        }
        return Optional.of(new Date(timestamp));
    }

    public void setLowerDateBoundary(@NonNull Date date) {
        storage.put(LOWER_DATE_BOUNDARY_KEY, date.getTime());
    }

    @NonNull
    public Optional<Date> getLowerDateBoundary() {
        Long timestamp = storage.getLong(LOWER_DATE_BOUNDARY_KEY);
        if (timestamp == null) {
            return Optional.empty();
        }
        return Optional.of(new Date(timestamp));
    }
} 
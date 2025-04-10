package com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata;

import androidx.annotation.NonNull;
import androidx.health.connect.client.records.Record;

import com.fjuul.sdk.core.entities.IStorage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HCSyncMetadataStore {
    private static final String KEY_PREFIX = "health_connect_sync.";
    private static final Duration SYNC_INTERVAL = Duration.ofHours(24);

    @NonNull
    private final IStorage storage;
    private final Map<String, Instant> syncMetadata = new HashMap<>();

    public HCSyncMetadataStore(@NonNull IStorage storage) {
        this.storage = storage;
    }

    public boolean isNeededToSync(@NonNull Class<? extends Record> recordType) {
        String key = getKeyForRecordType(recordType);
        Instant lastSync = syncMetadata.get(key);
        if (lastSync == null) {
            return true;
        }
        return Duration.between(lastSync, Instant.now()).compareTo(SYNC_INTERVAL) > 0;
    }

    public void saveSyncMetadata(@NonNull Class<? extends Record> recordType) {
        String key = getKeyForRecordType(recordType);
        syncMetadata.put(key, Instant.now());
        storage.set(key, String.valueOf(Instant.now().toEpochMilli()));
    }

    private String getKeyForRecordType(@NonNull Class<? extends Record> recordType) {
        return KEY_PREFIX + recordType.getSimpleName();
    }
}

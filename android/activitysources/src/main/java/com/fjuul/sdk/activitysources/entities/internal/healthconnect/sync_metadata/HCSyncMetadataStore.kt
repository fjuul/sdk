package com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata

import androidx.health.connect.client.records.Record
import com.fjuul.sdk.core.entities.IStorage
import java.time.Duration
import java.time.Instant

class HCSyncMetadataStore(private val storage: IStorage) {
    private val syncMetadata: MutableMap<String, Instant> = HashMap()

    fun isNeededToSync(recordType: Class<out Record?>): Boolean {
        val key = getKeyForRecordType(recordType)
        val lastSync = syncMetadata[key] ?: return true
        return Duration.between(lastSync, Instant.now()).compareTo(SYNC_INTERVAL) > 0
    }

    fun saveSyncMetadata(recordType: Class<out Record?>) {
        val key = getKeyForRecordType(recordType)
        syncMetadata[key] = Instant.now()
        storage[key] = Instant.now().toEpochMilli().toString()
    }

    private fun getKeyForRecordType(recordType: Class<out Record?>): String {
        return KEY_PREFIX + recordType.simpleName
    }

    companion object {
        private const val KEY_PREFIX = "health_connect_sync."
        private val SYNC_INTERVAL: Duration = Duration.ofHours(24)
    }
}

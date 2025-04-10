package com.fjuul.sdk.activitysources.entities.internal.healthconnect.sync_metadata

import java.time.Instant

/**
 * Base class for Health Connect sync metadata.
 * @property lastSyncTime The timestamp of the last successful sync
 */
sealed class HCSyncMetadata(
    open val lastSyncTime: Instant
)

/**
 * Metadata for intraday data sync (steps, heart rate, calories).
 */
data class HCIntradaySyncMetadata(
    override val lastSyncTime: Instant
) : HCSyncMetadata(lastSyncTime)

/**
 * Metadata for daily data sync (resting heart rate).
 */
data class HCDailySyncMetadata(
    override val lastSyncTime: Instant
) : HCSyncMetadata(lastSyncTime)

/**
 * Metadata for profile data sync (height, weight).
 */
data class HCProfileSyncMetadata(
    override val lastSyncTime: Instant
) : HCSyncMetadata(lastSyncTime) 
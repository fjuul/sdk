package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Represents a daily summary payload for Health Connect.
 *
 * @param date string in YYYY-MM-DD format
 * @param dataOrigins list of source packages (e.g. ["healthconnect"])
 * @param steps optional total steps for the day
 * @param restingHeartRate optional statistical summary of resting HR
 */
data class HealthConnectDailiesPayload(
    val date: String,
    val dataOrigins: List<String>,
    val steps: Int? = null,
    val restingHeartRate: StatisticalValue? = null
)


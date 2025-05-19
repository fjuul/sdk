package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Daily summary payload.
 *
 * @property date day for which data is reported, format: YYYY-MM-DD
 * @property dataOrigins source packages contributing to the data
 * @property steps total steps for the day (optional)
 * @property restingHeartRate resting heart rate statistics (optional)
 */
@JsonClass(generateAdapter = true)
data class HealthConnectDailiesPayload(
    val date: String,
    val dataOrigins: List<String>,
    val steps: Int? = null,
    val restingHeartRate: StatisticalValue? = null
)

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Single intraday entry with ISO timestamp, sources and metric values.
 * `start` is ISO datetime string, e.g. "2025-05-27T12:00:00Z"
 */
data class IntradayEntry(
    val start: String,
    val dataOrigins: List<String>,
    val metrics: Map<String, Double>
)

/** Payload for intraday upload. */
data class HealthConnectIntradayPayload(
    val entries: List<IntradayEntry>
)

/**
 * Single daily summary record.
 * `date` is ISO date string, e.g. "2025-05-27"
 */
data class DailyEntry(
    val date: String,
    val dataOrigins: List<String>,
    val steps: Long? = null,
    val restingHeartRate: StatisticalAggregateValue? = null
)

/** Statistical aggregate for heart-rate. */
data class StatisticalAggregateValue(
    val min: Double?,
    val avg: Double?,
    val max: Double?
)

/** Payload for daily upload. */
data class HealthConnectDailiesPayload(
    val entries: List<DailyEntry>
)

/** Payload for profile upload. */
data class HealthConnectProfilePayload(
    val height: Double? = null, // in cm
    val weight: Double? = null  // in kg
)

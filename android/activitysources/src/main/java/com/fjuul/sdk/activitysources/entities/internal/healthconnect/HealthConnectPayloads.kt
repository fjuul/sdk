package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Payload for intraday upload.
 *
 * - totalCalories: cumulative kcal per minute
 * - activeCalories: active kcal per minute
 * - heartrate: statistical hr per minute
 */
data class HealthConnectIntradayPayload(
    val totalCalories: MetricData<ValueEntry>?,
    val activeCalories: MetricData<ValueEntry>?,
    val heartrate: MetricData<HeartRateEntry>?
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

/**
 * Heart rate entry: timestamp + min/avg/max.
 */
data class HeartRateEntry(
    val start: String? = null,
    val min: Double? = null,
    val avg: Double? = null,
    val max: Double? = null
)

/**
 * Single daily summary record.
 * `date` is ISO date string, e.g. "2025-05-27"
 * restingHeartRate is a StatisticalEntry with null `start`.
 */
data class DailyEntry(
    val date: String,
    val dataOrigins: List<String>,
    val steps: Long? = null,
    val restingHeartRate: HeartRateEntry? = null
)

/**
 * A simple timestamp + single value.
 * `start` is ISO datetime string, e.g. "2025-05-27T00:00:00Z"
 */
data class ValueEntry(
    val start: String,
    val value: Double
)

/**
 * Container for one metric’s intraday series.
 */
data class MetricData<TEntry>(
    val dataOrigins: List<String>,
    val entries: List<TEntry>
)

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Single cumulative intraday entry: timestamp + total value.
 */
data class CumulativeEntry(
    val start: String,  // ISO datetime
    val totalCalories: Double? = null,
    val activeCalories: Double? = null
)

/**
 * Single statistical intraday entry: timestamp + min/avg/max.
 */
data class StatisticalEntry(
    val start: String? = null,  // ISO datetime, e.g. "2025-05-27T00:00:00Z"
    val min: Double? = null,
    val avg: Double? = null,
    val max: Double? = null
)

/**
 * Generic container for intraday data of type T (either CumulativeEntry or StatisticalEntry).
 */
data class IntradayDataBase<TEntry>(
    val dataOrigins: List<String>,
    val entries: List<TEntry>
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
    val restingHeartRate: StatisticalEntry? = null
)

/**
 * Payload for intraday upload, containing optional cumulative and/or statistical data.
 */
data class HealthConnectIntradayPayload(
    val cumulative: IntradayDataBase<CumulativeEntry>?,
    val statistical: IntradayDataBase<StatisticalEntry>?
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

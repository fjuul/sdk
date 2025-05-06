package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.util.Date

/**
 * Represents an entry in the intraday data with either a single value or statistical values.
 *
 * @property start The start time of the measurement
 * @property value The single value for cumulative metrics (e.g., calories)
 * @property min The minimum value for statistical metrics (e.g., heart rate)
 * @property avg The average value for statistical metrics (e.g., heart rate)
 * @property max The maximum value for statistical metrics (e.g., heart rate)
 */
data class HealthConnectIntradayEntry(
    val start: Date,
    val value: Double? = null,
    val min: Double? = null,
    val avg: Double? = null,
    val max: Double? = null
)

/**
 * Represents intraday health data to be uploaded to the Fjuul backend.
 * This includes both cumulative metrics (like calories) and statistical metrics (like heart rate).
 *
 * @property dataOrigins List of data sources that contributed to this data
 * @property entries List of intraday entries with their measurements
 */
data class HealthConnectIntradayData(
    val dataOrigins: List<String>,
    val entries: List<HealthConnectIntradayEntry>
) 
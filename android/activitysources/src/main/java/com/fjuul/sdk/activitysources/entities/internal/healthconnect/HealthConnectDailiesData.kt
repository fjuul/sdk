package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.util.Date

/**
 * Represents statistical values for a metric (e.g., heart rate) over a period.
 *
 * @property min The minimum value observed
 * @property avg The average value observed
 * @property max The maximum value observed
 */
data class RestingHeartRate(
    val min: Double,
    val avg: Double,
    val max: Double
)

/**
 * Represents daily health data to be uploaded to the Fjuul backend.
 * This includes metrics that are aggregated on a daily basis.
 *
 * @property date The date for which this data applies
 * @property dataOrigins List of data sources that contributed to this data
 * @property steps The total number of steps for the day
 * @property restingHeartRate The resting heart rate statistics for the day
 */
data class HealthConnectDailiesData(
    val date: Date,
    val dataOrigins: List<String>,
    val steps: Int? = null,
    val restingHeartRate: RestingHeartRate? = null
) 
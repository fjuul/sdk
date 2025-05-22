package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import java.time.Instant

/**
 * A unified representation of a single data value from Health Connect.
 *
 * This intermediate format is used before aggregating into upload payloads.
 */
data class HealthConnectDataPoint(
    val type: FitnessMetricsType,
    val startTime: Instant,
    val endTime: Instant?,
    val value: Double
)

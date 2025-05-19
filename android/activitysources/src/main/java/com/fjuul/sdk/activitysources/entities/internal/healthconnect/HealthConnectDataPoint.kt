package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import java.time.Instant

data class HealthConnectDataPoint(
    val type: FitnessMetricsType,
    val startTime: Instant,
    val endTime: Instant?,
    val value: Double
)

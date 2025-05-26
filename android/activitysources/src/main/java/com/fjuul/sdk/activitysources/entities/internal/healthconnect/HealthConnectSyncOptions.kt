package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import java.time.LocalDate

/**
 * Configuration class defining what Health Connect data to sync,
 * and the time range for the data query.
 *
 * This object is passed from ViewModel/UI to ActivitySource.
 */
data class HealthConnectSyncOptions(
    val metrics: Set<FitnessMetricsType>,
    val timeRangeStart: LocalDate,
    val timeRangeEnd: LocalDate
)

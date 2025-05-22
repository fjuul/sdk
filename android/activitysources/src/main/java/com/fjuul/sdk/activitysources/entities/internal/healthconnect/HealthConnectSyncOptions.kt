package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.LocalDate

/**
 * Configuration class defining what Health Connect data to sync,
 * and the time range for the data query.
 *
 * This object is passed from ViewModel/UI to ActivitySource.
 */
data class HealthConnectSyncOptions(
    val readSteps: Boolean? = false,
    val readCalories: Boolean? = false,
    val readHeartRate: Boolean? = false,
    val readHeight: Boolean? = false,
    val readWeight: Boolean? = false,
    val timeRangeStart: LocalDate,
    val timeRangeEnd: LocalDate
)

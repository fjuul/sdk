package com.fjuul.sdk.activitysources.entities.internal.healthconnect

data class HealthConnectSyncOptions(
    val readSteps: Boolean = false,
    val readCalories: Boolean = false,
    val readHeartRate: Boolean = false,
    val readHeight: Boolean = false,
    val readWeight: Boolean = false,
    val timeRangeStart: Long,
    val timeRangeEnd: Long
)

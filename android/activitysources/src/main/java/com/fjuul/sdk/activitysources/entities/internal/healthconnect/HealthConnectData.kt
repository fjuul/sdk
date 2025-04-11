package com.fjuul.sdk.activitysources.entities.internal.healthconnect

data class HealthConnectStepsData(
    val count: Long,
    val startTime: Long,
    val endTime: Long
)

data class HealthConnectHeartRateData(
    val beatsPerMinute: Long,
    val startTime: Long,
    val endTime: Long
)

data class HealthConnectCaloriesData(
    val calories: Double,
    val startTime: Long,
    val endTime: Long
)

data class HealthConnectProfileData(
    val weight: Double? = null,
    val height: Double? = null
)

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

data class HealthConnectTimeInterval(
    val startTime: Instant,
    val endTime: Instant,
)

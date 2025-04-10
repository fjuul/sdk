package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

/**
 * Represents a heart rate record from Health Connect.
 */
data class HCHeartRateDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCIntradayDataPoint()

/**
 * Represents a resting heart rate record from Health Connect.
 */
data class HCRestingHeartRateDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint()

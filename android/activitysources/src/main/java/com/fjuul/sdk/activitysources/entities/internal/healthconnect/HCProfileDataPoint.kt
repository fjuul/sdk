package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

/**
 * Represents a height measurement record from Health Connect.
 */
data class HCHeightDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint()

/**
 * Represents a weight measurement record from Health Connect.
 */
data class HCWeightDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint() 
package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

/**
 * Represents a total calories burned record from Health Connect.
 */
data class HCTotalCaloriesDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint()

/**
 * Represents an active calories burned record from Health Connect.
 */
data class HCActiveCaloriesDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint()

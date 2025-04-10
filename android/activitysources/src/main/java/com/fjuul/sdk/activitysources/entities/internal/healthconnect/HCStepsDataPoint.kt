package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

/**
 * Represents a steps count record from Health Connect.
 */
data class HCStepsDataPoint(
    override val start: Instant,
    override val value: Double,
    override val dataSources: Set<String>
) : HCDailyDataPoint()

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.time.Instant

/**
 * Base class for all Health Connect data points.
 */
abstract class HCDataPoint {
    abstract val start: Instant
    abstract val end: Instant?
    abstract val value: Double
    abstract val dataSources: Set<String>
}

/**
 * Base class for daily data points (e.g. daily steps, daily calories).
 */
abstract class HCDailyDataPoint : HCDataPoint() {
    final override val end: Instant? = null
}

/**
 * Base class for intraday data points (e.g. heart rate samples).
 */
abstract class HCIntradayDataPoint : HCDataPoint() {
    final override val end: Instant
        get() = start
}

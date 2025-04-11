package com.fjuul.sdk.activitysources.healthconnect.model

import java.time.Instant

/**
 * Represents a calories entry in a time series.
 */
public data class CaloriesEntry(
    override val start: Instant,
    override val value: Double
) : HealthConnectCumulativeEntry

/**
 * Represents a batch of calories data for a specific time period.
 */
public data class CaloriesData(
    override val dataOrigins: Set<String>,
    override val entries: List<CaloriesEntry>
) : HealthConnectCumulativeAggregateIntradayData {
    public val isActive: Boolean = false // TODO: Determine based on data source
} 
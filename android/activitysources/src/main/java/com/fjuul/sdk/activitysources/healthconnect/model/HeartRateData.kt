package com.fjuul.sdk.activitysources.healthconnect.model

import java.time.Instant

/**
 * Represents a heart rate entry in a time series.
 */
public data class HeartRateEntry(
    override val start: Instant,
    override val min: Double,
    override val avg: Double,
    override val max: Double
) : HealthConnectStatisticalEntry

/**
 * Represents a batch of heart rate data for a specific time period.
 */
public data class HeartRateData(
    override val dataOrigins: Set<String>,
    override val entries: List<HeartRateEntry>
) : HealthConnectStatisticalAggregateIntradayData 
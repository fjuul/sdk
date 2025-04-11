package com.fjuul.sdk.activitysources.healthconnect.model

import java.time.Instant

/**
 * Base interface for cumulative aggregate values (e.g. calories).
 */
public interface HealthConnectCumulativeAggregateValue {
    public val value: Double
}

/**
 * Base interface for statistical aggregate values (e.g. heart rate).
 */
public interface HealthConnectStatisticalAggregateValue {
    public val min: Double
    public val avg: Double
    public val max: Double
}

/**
 * Base interface for intraday entries.
 */
public interface HealthConnectIntradayEntryBase {
    public val start: Instant
}

/**
 * Interface for cumulative aggregate entries (e.g. calories).
 */
public interface HealthConnectCumulativeEntry : 
    HealthConnectIntradayEntryBase,
    HealthConnectCumulativeAggregateValue

/**
 * Interface for statistical aggregate entries (e.g. heart rate).
 */
public interface HealthConnectStatisticalEntry : 
    HealthConnectIntradayEntryBase,
    HealthConnectStatisticalAggregateValue

/**
 * Base interface for intraday data with data origins.
 */
public interface HealthConnectIntradayDataBase<TEntry> {
    public val dataOrigins: Set<String>
    public val entries: List<TEntry>
}

/**
 * Interface for cumulative aggregate intraday data (e.g. calories).
 */
public interface HealthConnectCumulativeAggregateIntradayData : 
    HealthConnectIntradayDataBase<HealthConnectCumulativeEntry>

/**
 * Interface for statistical aggregate intraday data (e.g. heart rate).
 */
public interface HealthConnectStatisticalAggregateIntradayData : 
    HealthConnectIntradayDataBase<HealthConnectStatisticalEntry> 
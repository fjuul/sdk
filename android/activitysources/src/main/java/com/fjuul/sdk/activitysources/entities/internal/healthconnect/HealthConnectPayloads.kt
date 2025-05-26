package com.fjuul.sdk.activitysources.entities.internal.healthconnect


import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord

/**
 * Payload for intraday synchronization.
 *
 * @property intradayStats List of hourly aggregated data buckets returned by Health Connect.
 */
data class HealthConnectIntradayData(
    val intradayStats: List<AggregationResultGroupedByDuration>
)

/**
 * Payload for daily synchronization.
 *
 * @property dailyStats List of daily aggregated data buckets returned by Health Connect.
 */
data class HealthConnectDailiesData(
    val dailyStats: List<AggregationResultGroupedByPeriod>
)

/**
 * Payload for profile synchronization.
 *
 * @property heights List of height records returned by Health Connect.
 * @property weights List of weight records returned by Health Connect.
 */
data class HealthConnectProfileData(
    val heights: List<HeightRecord>,
    val weights: List<WeightRecord>
)

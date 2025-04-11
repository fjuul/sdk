package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.util.Date

/**
 * Base sealed interface for all Health Connect data points.
 * This provides type-safe handling of different health metrics.
 */
sealed interface HealthConnectDataPoint {
    /**
     * The start time of the data point.
     */
    val startTime: Date

    /**
     * The end time of the data point.
     */
    val endTime: Date

    /**
     * The data source that provided this data point.
     * This can be null if the source is unknown.
     */
    val dataSource: String?
}

/**
 * Data point for steps count from Health Connect.
 *
 * @property startTime The start time of the steps measurement
 * @property endTime The end time of the steps measurement
 * @property dataSource The source of the steps data
 * @property count The number of steps during this time period
 */
data class StepsDataPoint(
    override val startTime: Date,
    override val endTime: Date,
    override val dataSource: String?,
    val count: Long
) : HealthConnectDataPoint

/**
 * Data point for heart rate measurements from Health Connect.
 *
 * @property startTime The start time of the heart rate measurement
 * @property endTime The end time of the heart rate measurement
 * @property dataSource The source of the heart rate data
 * @property beatsPerMinute The heart rate in beats per minute
 */
data class HeartRateDataPoint(
    override val startTime: Date,
    override val endTime: Date,
    override val dataSource: String?,
    val beatsPerMinute: Long
) : HealthConnectDataPoint

/**
 * Data point for calories burned from Health Connect.
 *
 * @property startTime The start time of the calories measurement
 * @property endTime The end time of the calories measurement
 * @property dataSource The source of the calories data
 * @property calories The number of calories burned during this time period
 */
data class CaloriesDataPoint(
    override val startTime: Date,
    override val endTime: Date,
    override val dataSource: String?,
    val calories: Double
) : HealthConnectDataPoint 
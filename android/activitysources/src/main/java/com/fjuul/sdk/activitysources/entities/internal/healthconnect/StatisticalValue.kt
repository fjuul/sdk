package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Represents a statistical value for daily metrics (e.g. resting heart rate).
 *
 * @param min minimum value recorded
 * @param avg average value
 * @param max maximum value
 */
data class StatisticalValue(
    val min: Double,
    val avg: Double,
    val max: Double
)

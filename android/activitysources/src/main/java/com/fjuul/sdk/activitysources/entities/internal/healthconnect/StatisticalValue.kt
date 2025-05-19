package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Represents a statistical value for daily metrics (e.g. resting heart rate).
 *
 * @param min minimum value recorded
 * @param avg average value
 * @param max maximum value
 */
@JsonClass(generateAdapter = true)
data class StatisticalValue(
    val min: Double,
    val avg: Double,
    val max: Double
)

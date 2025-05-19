package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Represents a statistical daily value (used in daily payload).
 *
 * @property min minimum value
 * @property avg average value
 * @property max maximum value
 */
@JsonClass(generateAdapter = true)
data class StatisticalValue(
    val min: Double,
    val avg: Double,
    val max: Double
)

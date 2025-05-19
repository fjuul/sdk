package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass
import java.util.Date

/**
 * Represents a statistical intraday entry (e.g. heart rate).
 *
 * @param start start timestamp of the window
 * @param min minimum value in that window
 * @param avg average value
 * @param max maximum value
 */
@JsonClass(generateAdapter = true)
data class IntradayStatisticalEntry(
    val start: Date,
    val min: Double,
    val avg: Double,
    val max: Double
)


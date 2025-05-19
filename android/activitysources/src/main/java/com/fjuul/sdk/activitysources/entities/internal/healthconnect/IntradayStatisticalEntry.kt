package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass
import java.util.Date

/**
 * Represents a statistical intraday entry (e.g. heart rate).
 *
 * @property start start timestamp of the measurement interval
 * @property min minimum value
 * @property avg average value
 * @property max maximum value
 */
@JsonClass(generateAdapter = true)
data class IntradayStatisticalEntry(
    val start: Date,
    val min: Double,
    val avg: Double,
    val max: Double
)

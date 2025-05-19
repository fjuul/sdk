package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass
import java.util.Date

/**
 * Represents a cumulative intraday entry (e.g. calories burned).
 *
 * @property start start timestamp of the measurement interval
 * @property value total value during the interval
 */
@JsonClass(generateAdapter = true)
data class IntradayCumulativeEntry(
    val start: Date,
    val value: Double
)

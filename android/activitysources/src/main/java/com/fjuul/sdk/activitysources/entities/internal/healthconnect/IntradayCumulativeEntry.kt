package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import java.util.Date

/**
 * Represents a cumulative intraday entry (e.g. calories).
 *
 * @param start start timestamp of the measurement window
 * @param value summed value during that window
 */
data class IntradayCumulativeEntry(
    val start: Date,
    val value: Double
)

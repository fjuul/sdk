package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Generic container for intraday Health Connect data payloads.
 *
 * Used for both cumulative (e.g. calories) and statistical (e.g. heart rate) uploads.
 *
 * @param T either IntradayCumulativeEntry or IntradayStatisticalEntry
 * @param dataOrigins list of contributing data sources (typically ["healthconnect"])
 * @param entries list of intraday entries
 */
@JsonClass(generateAdapter = true)
data class HealthConnectIntradayPayload<T>(
    val dataOrigins: List<String>,
    val entries: List<T>
)


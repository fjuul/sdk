package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Generic container for intraday payloads (cumulative or statistical).
 *
 * @param T entry type: IntradayCumulativeEntry or IntradayStatisticalEntry
 * @property dataOrigins source packages contributing to the data
 * @property entries list of intraday entries
 */
@JsonClass(generateAdapter = true)
data class HealthConnectIntradayPayload<T>(
    val dataOrigins: List<String>,
    val entries: List<T>
)

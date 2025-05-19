package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * Represents user profile data (biometrics) synced from Health Connect.
 *
 * Both values are optional and will be omitted if null.
 *
 * @param height height in meters (optional)
 * @param weight weight in kilograms (optional)
 */
@JsonClass(generateAdapter = true)
data class HealthConnectProfilePayload(
    val height: Double? = null,
    val weight: Double? = null
)

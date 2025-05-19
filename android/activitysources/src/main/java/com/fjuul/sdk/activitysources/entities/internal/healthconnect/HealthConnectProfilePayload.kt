package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.squareup.moshi.JsonClass

/**
 * User profile data payload.
 *
 * @property height height in meters (optional)
 * @property weight weight in kilograms (optional)
 */
@JsonClass(generateAdapter = true)
data class HealthConnectProfilePayload(
    val height: Double? = null,
    val weight: Double? = null
)

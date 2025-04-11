package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Represents user profile data from Health Connect to be uploaded to the Fjuul backend.
 * This includes basic biometric data that doesn't change frequently.
 *
 * @property weight The user's weight in kilograms
 * @property height The user's height in meters
 */
data class HealthConnectProfileData(
    val weight: Double? = null,
    val height: Double? = null
) 
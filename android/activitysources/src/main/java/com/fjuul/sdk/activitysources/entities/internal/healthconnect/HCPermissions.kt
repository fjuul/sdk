package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*

/**
 * Health Connect permissions management.
 */
object HCPermissions {
    /**
     * Required permissions for reading health data.
     */
    val REQUIRED_PERMISSIONS = setOf(
        // Steps
        HealthPermission.getReadPermission(StepsRecord::class),
        // Heart rate
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        // Calories
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        // Profile
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )
} 
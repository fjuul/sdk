package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*

/**
 * Manages permission logic for Health Connect, dynamically based on requested sync options.
 */
class HealthConnectPermissionManager {

    fun getPermissionsToRequest(options: HealthConnectSyncOptions): Set<String> {
        val permissions = mutableSetOf<String>()

        if (options.readSteps) {
            permissions += HealthPermission.getReadPermission(StepsRecord::class)
        }
        if (options.readCalories) {
            permissions += HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        }
        if (options.readHeartRate) {
            permissions += HealthPermission.getReadPermission(HeartRateRecord::class)
        }
        if (options.readHeight) {
            permissions += HealthPermission.getReadPermission(HeightRecord::class)
        }
        if (options.readWeight) {
            permissions += HealthPermission.getReadPermission(WeightRecord::class)
        }

        return permissions
    }

    suspend fun hasAllPermissions(context: Context, options: HealthConnectSyncOptions): Boolean {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val required = getPermissionsToRequest(options)
        return granted.containsAll(required)
    }
}


package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.CommonException
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.HealthConnectPermissionsNotGrantedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages Health Connect permissions.
 * Handles permission checking and validation.
 */
class HealthConnectPermissionManager(private val context: Context) {
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    /**
     * Checks if all required permissions are granted.
     *
     * @return true if all permissions are granted, false otherwise
     * @throws CommonException if there's an error checking permissions
     */
    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            val missingPermissions = requiredPermissions - grantedPermissions
            if (missingPermissions.isNotEmpty()) {
                Log.w(TAG, "Missing Health Connect permissions: ${missingPermissions.joinToString()}")
            }
            grantedPermissions.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Health Connect permissions", e)
            throw CommonException("Failed to check Health Connect permissions: ${e.message}")
        }
    }

    /**
     * Ensures all required permissions are granted.
     *
     * @throws HealthConnectPermissionsNotGrantedException if any required permission is not granted
     */
    suspend fun ensurePermissions() {
        if (!hasAllPermissions()) {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            val missingPermissions = requiredPermissions - grantedPermissions
            Log.e(TAG, "Missing Health Connect permissions: ${missingPermissions.joinToString()}")
            throw HealthConnectPermissionsNotGrantedException("Missing Health Connect permissions: ${missingPermissions.joinToString()}")
        }
    }

    companion object {
        private const val TAG = "HealthConnectPermissionManager"
    }
} 
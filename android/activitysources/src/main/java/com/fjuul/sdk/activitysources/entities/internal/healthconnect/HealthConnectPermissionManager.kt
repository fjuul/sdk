package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.utils.runAsyncAndCallback
import com.fjuul.sdk.core.entities.Callback

private const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * Manages Health Connect SDK availability and user permissions based on
 * the configured set of fitness metrics.
 *
 * @param context             Application context used for HealthConnectClient.
 * @param healthConnectClient Pre-initialized HealthConnectClient for all data and permission operations.
 * @param allAvailableMetrics          The complete set of metrics that this SDK may request permissions for.
 */
class HealthConnectPermissionManager(
    private val context: Context,
    private val healthConnectClient: HealthConnectClient,
    private val allAvailableMetrics: Set<FitnessMetricsType>
) {

    /**
     * Returns the ActivityResultContract to request Health Connect permissions.
     */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    /**
     * Returns all permission strings required for the configured metrics.
     */
    fun requiredPermissions(): Set<String> =
        permissionsForMetrics(allAvailableMetrics)

    /**
     * Throws if Health Connect SDK is not installed or not supported.
     */
    fun ensureSdkAvailable() {
        when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> Unit
            Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK ->
                throw HealthConnectException.NotInstalledException()

            else ->
                throw HealthConnectException.NotSupportedException()
        }
    }

    /**
     * Checks asynchronously if permissions for the given metrics are all granted.
     * Delivers `true` to the callback if all are granted, or an exception if any are missing.
     */
    fun checkPermissions(
        metrics: Set<FitnessMetricsType>,
        callback: Callback<Boolean>
    ) = runAsyncAndCallback({
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        val required = permissionsForMetrics(metrics)
        if (!granted.containsAll(required)) {
            throw HealthConnectException.PermissionsNotGrantedException()
        }
        true
    }, callback)

    /**
     * Suspends and throws if permissions for the given metrics are not granted.
     */
    suspend fun ensurePermissionsGranted(metrics: Set<FitnessMetricsType>) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        val required = permissionsForMetrics(metrics)
        if (!granted.containsAll(required)) {
            throw HealthConnectException.PermissionsNotGrantedException()
        }
    }

    /**
     * Revokes all Health Connect permissions asynchronously.
     */
    fun revokeAllPermissions(callback: Callback<Unit>) =
        runAsyncAndCallback({
            healthConnectClient.permissionController.revokeAllPermissions()
        }, callback)

    /**
     * Builds the set of Health Connect permission strings required for the specified metrics.
     */
    private fun permissionsForMetrics(metrics: Set<FitnessMetricsType>): Set<String> =
        buildSet {
            if (FitnessMetricsType.INTRADAY_CALORIES in metrics) {
                add(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
                add(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
            }
            if (FitnessMetricsType.INTRADAY_HEART_RATE in metrics) {
                add(HealthPermission.getReadPermission(HeartRateRecord::class))
            }
            if (FitnessMetricsType.RESTING_HEART_RATE in metrics) {
                add(HealthPermission.getReadPermission(RestingHeartRateRecord::class))
            }
            if (FitnessMetricsType.STEPS in metrics) {
                add(HealthPermission.getReadPermission(StepsRecord::class))
            }
            if (FitnessMetricsType.HEIGHT in metrics) {
                add(HealthPermission.getReadPermission(HeightRecord::class))
            }
            if (FitnessMetricsType.WEIGHT in metrics) {
                add(HealthPermission.getReadPermission(WeightRecord::class))
            }
        }
}

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.utils.runCatchingResult
import com.fjuul.sdk.core.entities.Callback
import com.fjuul.sdk.core.entities.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * Manages Health Connect permissions and availability checks based on a set of fitness metrics.
 *
 * @param context              Android context used to initialize [HealthConnectClient].
 * @param healthConnectClient  Pre-initialized [HealthConnectClient] for all data & permission calls.
 * @param allAvailableMetrics  The complete set of metrics that the SDK may request permissions for.
 */
class HealthConnectPermissionManager(
    private val context: Context,
    private val healthConnectClient: HealthConnectClient,
    private val allAvailableMetrics: Set<FitnessMetricsType>
) {

    /**
     * Returns the [ActivityResultContract] that initiates a Health Connect permission request
     * for an arbitrary set of permission strings.
     *
     * Clients should use this contract with [registerForActivityResult].
     */
    fun requestPermissions(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    /**
     * Computes the minimal set of Health Connect permission strings required
     * to read the given [metrics].
     *
     * @param metrics The subset of [FitnessMetricsType] for which read access is needed.
     * @return A [Set] of permission strings from [HealthPermission.getReadPermission].
     */
    fun getPermissionsToRequest(metrics: Set<FitnessMetricsType>): Set<String> {
        val permissions = mutableSetOf<String>()
        if (metrics.contains(FitnessMetricsType.INTRADAY_STEPS)) {
            permissions += HealthPermission.getReadPermission(StepsRecord::class)
        }
        if (metrics.contains(FitnessMetricsType.INTRADAY_CALORIES)) {
            permissions += HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        }
        if (metrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE)) {
            permissions += HealthPermission.getReadPermission(HeartRateRecord::class)
        }
        if (metrics.contains(FitnessMetricsType.HEIGHT)) {
            permissions += HealthPermission.getReadPermission(HeightRecord::class)
        }
        if (metrics.contains(FitnessMetricsType.WEIGHT)) {
            permissions += HealthPermission.getReadPermission(WeightRecord::class)
        }
        return permissions
    }

    /**
     * Returns the full set of required permissions for the SDK’s configured metrics.
     *
     * @return A [Set] of all permission strings for [allAvailableMetrics].
     */
    fun getAllRequiredPermissions(): Set<String> =
        getPermissionsToRequest(allAvailableMetrics)

    /**
     * Checks whether the Health Connect app is installed, supported by the OS, or not supported.
     *
     * @return One of [HealthConnectAvailability.INSTALLED], [SUPPORTED], or [NOT_SUPPORTED].
     */
    fun getAvailability(): HealthConnectAvailability = when {
        isInstalled() -> HealthConnectAvailability.INSTALLED
        isSupported() -> HealthConnectAvailability.SUPPORTED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }

    /**
     * Asynchronously verifies whether *all* of the SDK’s required permissions are granted.
     *
     * @param callback Receives a [Result] containing `true` if all are granted,
     *                 `false` if any are missing, or an exception if the check fails.
     */
    fun areAllPermissionsGranted(callback: Callback<Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result: Result<Boolean> = runCatchingResult {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val required = getPermissionsToRequest(allAvailableMetrics)
                granted.containsAll(required)
            }
            withContext(Dispatchers.Main) {
                callback.onResult(result)
            }
        }
    }

    /**
     * Asynchronously verifies whether the specified subset of [metrics] has all required permissions.
     *
     * @param metrics  Subset of [FitnessMetricsType] to check.
     * @param callback Receives a [Result] containing `true` if all are granted,
     *                 `false` if any are missing, or an exception if the check fails.
     */
    fun areRequiredPermissionsGranted(
        metrics: Set<FitnessMetricsType>,
        callback: Callback<Boolean>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result: Result<Boolean> = runCatchingResult {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val required = getPermissionsToRequest(metrics)
                granted.containsAll(required)
            }
            withContext(Dispatchers.Main) {
                callback.onResult(result)
            }
        }
    }

    /**
     * Asynchronously returns the full set of already-granted Health Connect permissions.
     *
     * @param callback Receives a [Result] with the granted permission strings,
     *                 or an exception if the query fails.
     */
    fun getGrantedPermissions(callback: Callback<Set<String>>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result: Result<Set<String>> = runCatchingResult {
                healthConnectClient.permissionController.getGrantedPermissions()
            }
            withContext(Dispatchers.Main) {
                callback.onResult(result)
            }
        }
    }

    /**
     * Asynchronously revokes *all* Health Connect permissions for this app.
     *
     * @param callback Receives a [Result] indicating success or the thrown exception.
     */
    fun revokeAllPermissions(callback: Callback<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result: Result<Unit> = runCatchingResult {
                healthConnectClient.permissionController.revokeAllPermissions()
            }
            withContext(Dispatchers.Main) {
                callback.onResult(result)
            }
        }
    }

    @ChecksSdkIntAtLeast(api = MIN_SUPPORTED_SDK)
    private fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

    private fun isInstalled(): Boolean =
        HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE

    private fun isFeatureAvailable(feature: Int): Boolean =
        healthConnectClient.features.getFeatureStatus(feature) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
}

/**
 * Indicates Health Connect installation/support status on the device.
 */
enum class HealthConnectAvailability {
    /** Health Connect app is installed and available. */
    INSTALLED,

    /** Health Connect is supported by the OS but not installed. */
    SUPPORTED,

    /** Health Connect is not supported on this OS version. */
    NOT_SUPPORTED
}

package com.fjuul.sdk.activitysources.entities

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectDataManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectPermissionManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.activitysources.utils.runAndCallback
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.Callback

/**
 * [ActivitySource] implementation for Android Health Connect.
 *
 * Supports:
 *  - intraday sync (calories, heart rate)
 *  - daily sync (steps, resting heart rate)
 *  - profile sync (height, weight)
 *
 * Clients must call [initialize] before accessing the singleton via [getInstance].
 *
 * @property dataManager        Internal manager for reading and uploading Health Connect data.
 * @property permissionManager  Manages permission requests, checks, and revocations.
 */
class HealthConnectActivitySource private constructor(
    private val dataManager: HealthConnectDataManager,
    private val permissionManager: HealthConnectPermissionManager
) : ActivitySource() {

    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    /**
     * Returns the [ActivityResultContract] used to request Health Connect permissions
     * for a set of permission strings.
     *
     * Client code should register this with [registerForActivityResult].
     */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        permissionManager.requestPermissions()

    /**
     * Returns the full set of Health Connect permission strings that this SDK
     * is configured to request.
     *
     * @return A [Set] of permission strings corresponding to the SDK’s metrics.
     */
    fun getAllRequiredPermissions(): Set<String> =
        permissionManager.getAllRequiredPermissions()

    /**
     * Asynchronously checks whether *all* of the SDK’s required Health Connect permissions
     * have already been granted.
     *
     * @param callback Receives a [Result] containing `true` if all are granted,
     *                 `false` if any are missing, or failure(exception).
     */
    fun areAllPermissionsGranted(callback: Callback<Boolean>) =
        permissionManager.areAllPermissionsGranted(callback)

    /**
     * Asynchronously checks whether the specified subset of [metrics] has all required permissions.
     *
     * @param metrics  The subset of [FitnessMetricsType] to verify.
     * @param callback Receives a [Result] containing `true` if all are granted,
     *                 `false` if any are missing, or failure(exception).
     */
    fun areRequiredPermissionsGranted(
        metrics: Set<FitnessMetricsType>,
        callback: Callback<Boolean>
    ) = permissionManager.areRequiredPermissionsGranted(metrics, callback)

    /**
     * Asynchronously retrieves the set of already-granted Health Connect permission strings.
     *
     * @param callback Receives a [Result] with the granted strings or failure(exception).
     */
    fun getGrantedPermissions(callback: Callback<Set<String>>) =
        permissionManager.getGrantedPermissions(callback)

    /**
     * Asynchronously revokes *all* Health Connect permissions that this app has granted.
     *
     * @param callback Receives a [Result] indicating success or the thrown exception.
     */
    fun revokeAllPermissions(callback: Callback<Unit>) =
        permissionManager.revokeAllPermissions(callback)

    /**
     * Starts an intraday data synchronization (calories, heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying time ranges & metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    fun syncIntraday(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        runAndCallback({ dataManager.syncIntraday(options) }, callback)

    /**
     * Starts a daily data synchronization (steps, resting heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying date range & metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    fun syncDaily(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        runAndCallback({ dataManager.syncDaily(options) }, callback)

    /**
     * Starts a profile data synchronization (height, weight).
     *
     * @param options  The [HealthConnectSyncOptions] specifying which fields to sync.
     * @param callback Receives a [Result]<Boolean> where `true` indicates data updated,
     *                 `false` means no changes, or failure(exception).
     */
    fun syncProfile(options: HealthConnectSyncOptions, callback: Callback<Boolean>) =
        runAndCallback({ dataManager.syncProfile(options) }, callback)


    companion object {
        @Volatile
        private var instance: HealthConnectActivitySource? = null

        /**
         * Initializes the singleton instance with dependencies from [ApiClient].
         * Must be called once before [getInstance].
         *
         * @param apiClient  Application-wide [ApiClient] providing context & networking.
         * @param config     Configuration object specifying which fitness metrics to request.
         */
        @JvmStatic
        @Synchronized
        fun initialize(apiClient: ApiClient, config: ActivitySourcesManagerConfig) {
            if (instance == null) {
                val context = apiClient.appContext.applicationContext
                val hcClient = HealthConnectClient.getOrCreate(context)
                val service = ActivitySourcesService(apiClient)
                val dataMgr = HealthConnectDataManager(context, service)
                val permMgr = HealthConnectPermissionManager(
                    context = context,
                    healthConnectClient = hcClient,
                    allAvailableMetrics = config.collectableFitnessMetrics
                )
                instance = HealthConnectActivitySource(dataMgr, permMgr)
            }
        }

        /**
         * Returns the initialized singleton instance, or throws if not yet initialized.
         */
        @JvmStatic
        fun getInstance(): HealthConnectActivitySource =
            instance ?: throw IllegalStateException(
                "HealthConnectActivitySource must be initialized first"
            )
    }
}

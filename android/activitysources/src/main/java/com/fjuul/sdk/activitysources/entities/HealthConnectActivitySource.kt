package com.fjuul.sdk.activitysources.entities

import androidx.health.connect.client.HealthConnectClient
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource.Companion.getInstance
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource.Companion.initialize
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectDataManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectPermissionManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.activitysources.utils.runAsyncAndCallback
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

    /**
     * Starts an intraday data synchronization (calories, heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying time ranges & metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    fun syncIntraday(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        runAsyncAndCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncIntraday(options)
        }, callback)

    /**
     * Starts a daily data synchronization (steps, resting heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying date range & metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    fun syncDaily(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        runAsyncAndCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncDaily(options)
        }, callback)

    /**
     * Starts a profile data synchronization (height, weight).
     *
     * @param options  The [HealthConnectSyncOptions] specifying which fields to sync.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    fun syncProfile(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        runAsyncAndCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncProfile(options)
        }, callback)


    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    fun getPermissionManager(): HealthConnectPermissionManager = permissionManager

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
                val dataMgr = HealthConnectDataManager(hcClient, service)
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

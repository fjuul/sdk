package com.fjuul.sdk.activitysources.entities

import androidx.health.connect.client.HealthConnectClient
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource.Companion.getInstance
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource.Companion.initialize
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectDataManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectPermissionManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.Callback
import com.fjuul.sdk.core.entities.IStorage
import com.fjuul.sdk.core.entities.Result
import com.fjuul.sdk.core.utils.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
    private val permissionManager: HealthConnectPermissionManager,
    private val storage: IStorage,
) : ActivitySource(), AutoCloseable {

    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
    )
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher + CoroutineName("SingleThreadScope"))

    private val mutex = Mutex()
    private var currentJob: Job? = null

    var lowerDateBoundary: Date? = null

    /**
     * Starts an intraday data synchronization (calories, heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        executeWithCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncIntraday(options, lowerDateBoundary)
        }, callback)

    /**
     * Starts a daily data synchronization (steps, resting heart rate).
     *
     * @param options  The [HealthConnectSyncOptions] specifying metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    suspend fun syncDaily(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        executeWithCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncDaily(options, lowerDateBoundary)
        }, callback)

    /**
     * Starts a profile data synchronization (height, weight).
     *
     * @param options  The [HealthConnectSyncOptions] specifying metrics.
     * @param callback Receives a [Result]<Unit> indicating success or failure(exception).
     */
    suspend fun syncProfile(options: HealthConnectSyncOptions, callback: Callback<Unit>) =
        executeWithCallback({
            permissionManager.ensureSdkAvailable()
            permissionManager.ensurePermissionsGranted(options.metrics)
            dataManager.syncProfile(options, lowerDateBoundary)
        }, callback)


    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    fun getPermissionManager(): HealthConnectPermissionManager = permissionManager

    fun forInternalUseOnly_clearChangesTokens() {
        storage.set(HealthConnectDataManager.HEART_RATE_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.TOTAL_CALORIES_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.ACTIVE_CALORIES_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.RESTING_HEART_RATE_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.STEPS_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.HEIGHT_CHANGES_TOKEN, "")
        storage.set(HealthConnectDataManager.WEIGHT_CHANGES_TOKEN, "")
    }

    suspend fun <T : Any> executeWithCallback(
        block: suspend () -> T,
        callback: Callback<T>
    ) {
        mutex.withLock {
            currentJob?.join()
            val deferred = scope.async {
                val result: Result<T> = try {
                    Logger.get().d("executeWithCallback: Start job")
                    Result.value(block())
                } catch (e: Throwable) {
                    Result.error(e)
                } finally {
                    Logger.get().d("executeWithCallback: End job")
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(result)
                }
            }
            currentJob = deferred
            deferred.await()
        }
    }

    override fun close() {
        scope.cancel()
        executor.shutdown()
    }

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
        fun initialize(
            apiClient: ApiClient,
            config: ActivitySourcesManagerConfig,
            storage: IStorage
        ) {
            if (instance == null) {
                val context = apiClient.appContext.applicationContext
                val hcClient = HealthConnectClient.getOrCreate(context)
                val service = ActivitySourcesService(apiClient)
                val dataMgr = HealthConnectDataManager(hcClient, service, apiClient.storage)
                val permMgr = HealthConnectPermissionManager(
                    context = context,
                    healthConnectClient = hcClient,
                    allAvailableMetrics = config.collectableFitnessMetrics
                )
                instance = HealthConnectActivitySource(dataMgr, permMgr, storage)
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

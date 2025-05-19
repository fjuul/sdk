package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.fjuul.sdk.activitysources.entities.ActivitySource
import com.fjuul.sdk.activitysources.entities.TrackerValue
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectActivitySource.Companion.getInstance
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectActivitySource.Companion.initialize
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.Callback
import com.fjuul.sdk.core.entities.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ActivitySource implementation for Android Health Connect.
 *
 * Supports:
 * - intraday sync (calories, heart rate)
 * - daily sync (steps, resting HR)
 * - profile sync (height, weight)
 *
 * This class must be initialized via [initialize] before calling [getInstance].
 * Compatible with ActivitySourceResolver and TrackerConnection system.
 */
class HealthConnectActivitySource private constructor(
    private val context: Context,
    private val service: ActivitySourcesService
) : ActivitySource() {

    private val dataManager = HealthConnectDataManager(context, service)
    private val permissionManager = HealthConnectPermissionManager()

    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun getPermissionsToRequest(options: HealthConnectSyncOptions): Set<String> {
        return permissionManager.getPermissionsToRequest(options)
    }

    fun hasPermissions(options: HealthConnectSyncOptions, callback: Callback<Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            val granted = permissionManager.hasAllPermissions(context, options)
            callback.onResult(Result.value(granted))
        }
    }

    fun syncIntraday(options: HealthConnectSyncOptions, callback: Callback<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = dataManager.syncIntraday(options)
            callback.onResult(result)
        }
    }

    fun syncDaily(options: HealthConnectSyncOptions, callback: Callback<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = dataManager.syncDaily(options)
            callback.onResult(result)
        }
    }

    fun syncProfile(options: HealthConnectSyncOptions, callback: Callback<Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = dataManager.syncProfile(options)
            callback.onResult(result)
        }
    }

    companion object {
        @Volatile
        private var instance: HealthConnectActivitySource? = null

        /**
         * Initializes the singleton instance with dependencies from ApiClient.
         * Must be called before getInstance().
         */
        @JvmStatic
        @Synchronized
        fun initialize(client: ApiClient) {
            if (instance == null) {
                val context = client.appContext
                val service = ActivitySourcesService(client)
                instance = HealthConnectActivitySource(context, service)
            }
        }

        /**
         * Returns the singleton instance. Throws if not initialized.
         */
        @JvmStatic
        fun getInstance(): HealthConnectActivitySource {
            return instance
                ?: throw IllegalStateException("HealthConnectActivitySource must be initialized first")
        }
    }
}

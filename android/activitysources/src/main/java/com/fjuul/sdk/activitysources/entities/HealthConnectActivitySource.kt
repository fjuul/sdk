package com.fjuul.sdk.activitysources.entities

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectDataManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectPermissionManager
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.Callback
import com.fjuul.sdk.core.entities.Result
import com.fjuul.sdk.core.exceptions.FjuulException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * Activity source for Health Connect integration.
 */
class HealthConnectActivitySource private constructor(
    private val context: Context,
    private val collectableMetrics: Set<FitnessMetricsType>,
    private val service: ActivitySourcesService
) : ActivitySource() {

    private val permissionManager = HealthConnectPermissionManager(context)
    internal val dataManager = HealthConnectDataManager(context, service)

    companion object {
        @Volatile
        private var instance: HealthConnectActivitySource? = null

        @JvmStatic
        fun initialize(client: ApiClient, config: ActivitySourcesManagerConfig) {
            instance = HealthConnectActivitySource(
                client.appContext,
                config.collectableFitnessMetrics,
                ActivitySourcesService(client)
            )
        }

        @JvmStatic
        fun getInstance(): HealthConnectActivitySource {
            return instance
                ?: throw IllegalStateException("You must initialize ActivitySourceManager first before use HealthConnectActivitySource")
        }
    }

    fun buildRequestPermissionsIntent(): Intent {
        val permissions = mapMetricsToPermissions(collectableMetrics)
        return HealthConnectClient.getOrCreate(context)
            .permissionController
            .createRequestPermissionIntent(permissions)
    }

    fun handlePermissionsResult(@NonNull callback: Callback<Void>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                permissionManager.ensurePermissions()
                val result = service.connect(trackerValue.value).execute()
                if (result.isError) {
                    callback.onResult(Result.error(result.error))
                } else {
                    callback.onResult(Result.value(null))
                }
            } catch (e: Exception) {
                callback.onResult(Result.error(FjuulException(e.message)))
            }
        }
    }

    suspend fun uploadIntradayData(startTime: Long, endTime: Long) {
        dataManager.uploadIntradayData(startTime, endTime)
    }

    suspend fun uploadDailyData(date: Date) {
        dataManager.uploadDailyData(date)
    }

    suspend fun uploadProfileData() {
        dataManager.uploadProfileData()
    }

    fun arePermissionsGranted(): Boolean = runBlocking { permissionManager.hasAllPermissions() }

    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    private val trackerValue: TrackerValue
        get() = TrackerValue.HEALTH_CONNECT

    private fun mapMetricsToPermissions(metrics: Set<FitnessMetricsType>): Set<HealthPermission> {
        val perms = mutableSetOf<HealthPermission>()
        if (metrics.contains(FitnessMetricsType.INTRADAY_STEPS)) {
            perms.add(HealthPermission.getReadPermission(StepsRecord::class))
        }
        if (metrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE)) {
            perms.add(HealthPermission.getReadPermission(HeartRateRecord::class))
        }
        if (metrics.contains(FitnessMetricsType.INTRADAY_CALORIES)) {
            perms.add(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
        }
        if (metrics.contains(FitnessMetricsType.WEIGHT)) {
            perms.add(HealthPermission.getReadPermission(WeightRecord::class))
        }
        if (metrics.contains(FitnessMetricsType.HEIGHT)) {
            perms.add(HealthPermission.getReadPermission(HeightRecord::class))
        }
        return perms
    }
}

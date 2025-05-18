package com.fjuul.sdk.activitysources.entities

import android.content.Context
import androidx.annotation.NonNull
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectDataManager
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectPermissionManager
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import java.util.Date

/**
 * ActivitySource implementation for Health Connect.
 * Provides simple APIs to verify permissions and to synchronize data
 * with the Fjuul backend using [HealthConnectDataManager].
 */
class HealthConnectActivitySource private constructor(
    private val permissionManager: HealthConnectPermissionManager,
    private val dataManager: HealthConnectDataManager
) : ActivitySource() {

    override fun getTrackerValue(): TrackerValue = TrackerValue.HEALTH_CONNECT

    /** Check that all required Health Connect permissions are granted. */
    suspend fun hasAllPermissions(): Boolean = permissionManager.hasAllPermissions()

    /**
     * Upload intraday data to the backend for the given time range.
     * @param startTime start timestamp in millis
     * @param endTime end timestamp in millis
     */
    suspend fun syncIntraday(startTime: Long, endTime: Long) {
        dataManager.uploadIntradayData(startTime, endTime)
    }

    /**
     * Upload daily aggregated data for the specified date.
     */
    suspend fun syncDaily(date: Date) {
        dataManager.uploadDailyData(date)
    }

    /** Upload profile information such as height and weight. */
    suspend fun syncProfile() {
        dataManager.uploadProfileData()
    }

    companion object {
        @Volatile
        private var instance: HealthConnectActivitySource? = null

        /**
         * Initialize the singleton instance. Should be called from
         * [ActivitySourcesManager.initialize].
         */
        @JvmStatic
        fun initialize(@NonNull context: Context, @NonNull service: ActivitySourcesService) {
            instance = HealthConnectActivitySource(
                HealthConnectPermissionManager(context),
                HealthConnectDataManager(context, service)
            )
        }

        /**
         * Get the initialized instance of [HealthConnectActivitySource].
         * @throws IllegalStateException if not initialized via [initialize]
         */
        @JvmStatic
        fun getInstance(): HealthConnectActivitySource {
            return instance ?: throw IllegalStateException(
                "You must initialize ActivitySourcesManager first before use HealthConnectActivitySource"
            )
        }
    }
}

package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import com.fjuul.sdk.activitysources.entities.HealthConnectIntradaySyncOptions
import com.fjuul.sdk.activitysources.entities.HealthConnectProfileSyncOptions
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCSynchronizableProfileParams
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCUploadData
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCCalorieDataPoint
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCDataConverter
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCHeartRateSummaryDataPoint
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HCStepsDataPoint
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.utils.Logger
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Manages synchronization of user data from Health Connect.
 * Supports both intraday metrics (steps, calories, heart rate) and profile data (height, weight).
 *
 * Delegates data access to [HealthConnectService], converts records to DTOs,
 * and uploads them to backend via [ActivitySourcesService].
 *
 * Tokens are stored to perform incremental syncing.
 *
 * @param context Android context
 * @param healthService Service for accessing Health Connect data
 * @param activitySourcesService API service to send data to backend
 * @param apiClient Provides local storage access for token persistence
 */
class HCDataManager(
    private val context: Context,
    private val healthService: HealthConnectService,
    private val activitySourcesService: ActivitySourcesService,
    private val apiClient: ApiClient
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val MAX_DAYS = 30
    private val PROFILE_TOKEN_KEY = "hc-profile-changes-token"
    private val INTRADAY_TOKEN_KEY = "hc-intraday-changes-token"

    /**
     * Syncs intraday metrics (steps, calories, heart rate) from Health Connect.
     * Uses change tokens to avoid duplicate uploads.
     * Converts raw records to SDK data points and uploads them to the backend.
     *
     * @param options contains the metric types to sync
     * @return a Task that completes when upload is successful or skipped (if no data)
     */
    fun syncIntradayMetrics(options: HealthConnectIntradaySyncOptions): Task<Void> {
        val taskSource = TaskCompletionSource<Void>()
        coroutineScope.launch {
            try {
                val token = apiClient.storage.get(INTRADAY_TOKEN_KEY)
                val records = if (token == null) {
                    healthService.getInitialRecords(options.metrics, MAX_DAYS)
                } else {
                    healthService.getChangeRecords(options.metrics, token)
                }

                if (records.records.isEmpty()) {
                    Logger.get().d("No new records to send")
                    taskSource.setResult(null)
                    return@launch
                }

                val steps = mutableListOf<HCStepsDataPoint>()
                val calories = mutableListOf<HCCalorieDataPoint>()
                val heartRates = mutableListOf<HCHeartRateSummaryDataPoint>()

                records.records.forEach { record ->
                    when (record) {
                        is androidx.health.connect.client.records.StepsRecord -> steps.add(
                            HCDataConverter.convertRecordToSteps(record)
                        )

                        is androidx.health.connect.client.records.HeartRateRecord -> heartRates.add(
                            HCDataConverter.convertRecordToHeartRateSummary(record)
                        )

                        is androidx.health.connect.client.records.TotalCaloriesBurnedRecord -> calories.add(
                            HCDataConverter.convertRecordToCalories(record)
                        )

                        else -> Logger.get()
                            .e("Unexpected record type: ${record::class.java.canonicalName}")
                    }
                }

                val uploadData = HCUploadData().apply {
                        stepsData = steps
                        heartRateData = heartRates
                        caloriesData = calories
                    }

                Logger.get().d("Uploading HC data: $uploadData")

                activitySourcesService.uploadHealthConnectData(uploadData).enqueue { _, result ->
                    activitySourcesService.uploadHealthConnectData(uploadData)
                        .enqueue { _, result ->
                            if (result.isError) {
                                val message = result.error?.message ?: "Unknown error"
                                val err = Exception("Failed to send data to the server: $message")
                                taskSource.setException(err)
                                return@enqueue
                            }
                        }

                    Logger.get().d("Succeeded to send HC data")
                    apiClient.storage.set(INTRADAY_TOKEN_KEY, records.nextToken)
                    taskSource.setResult(null)
                }
            } catch (ex: Exception) {
                taskSource.setException(ex)
            }
        }
        return taskSource.task
    }

    /**
     * Syncs profile metrics (height, weight) from Health Connect.
     * Only the most recent values are uploaded.
     * Change token is used to fetch only new or updated records.
     *
     * @param options contains the profile metrics to sync
     * @return a Task that completes when upload is successful or skipped (if no new data)
     */
    fun syncProfile(options: HealthConnectProfileSyncOptions): Task<Void> {
        val taskSource = TaskCompletionSource<Void>()
        coroutineScope.launch {
            try {
                val token = apiClient.storage.get(PROFILE_TOKEN_KEY)
                val records = if (token == null) {
                    healthService.getInitialRecords(options.metrics, MAX_DAYS)
                } else {
                    healthService.getChangeRecords(options.metrics, token)
                }

                val profile = HCSynchronizableProfileParams()
                var newestHeight = Instant.EPOCH
                var newestWeight = Instant.EPOCH

                records.records.forEach { record ->
                    when (record) {
                        is androidx.health.connect.client.records.HeightRecord -> {
                            if (record.time.isAfter(newestHeight)) {
                                newestHeight = record.time
                                profile.setHeight(record.height.inMeters.toFloat())
                            }
                        }

                        is androidx.health.connect.client.records.WeightRecord -> {
                            if (record.time.isAfter(newestWeight)) {
                                newestWeight = record.time
                                profile.setWeight(record.weight.inKilograms.toFloat())
                            }
                        }

                        else -> Logger.get()
                            .e("Unexpected record type: ${record::class.java.canonicalName}")
                    }
                }

                if (profile.isEmpty) {
                    Logger.get().d("No new profile data to send")
                    taskSource.setResult(null)
                    return@launch
                }

                Logger.get().d("Uploading HC profile: $profile")

                activitySourcesService.updateProfileOnBehalfOfHealthConnect(profile)
                    .enqueue { _, result ->
                        if (result.isError) {
                            val message = result.error?.message ?: "Unknown error"
                            val err = Exception("Failed to send profile data: $message")
                            taskSource.setException(err)
                            return@enqueue
                        }

                        Logger.get().d("Succeeded to send profile")
                        apiClient.storage.set(PROFILE_TOKEN_KEY, records.nextToken)
                        taskSource.setResult(null)
                    }
            } catch (ex: Exception) {
                taskSource.setException(ex)
            }
        }
        return taskSource.task
    }
}

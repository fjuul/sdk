package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.entities.Result
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Handles the core logic for reading Health Connect data and uploading it to the backend.
 *
 * This class delegates:
 * - reading raw records to HealthConnectClientWrapper
 * - data aggregation and transformation to HealthConnectDataMapper
 * - actual HTTP uploads to ActivitySourcesService
 *
 * It provides methods to sync:
 * - Intraday data (e.g. calories, heart rate) in 1-hour batches
 * - Daily summaries (e.g. steps, resting heart rate)
 * - User profile data (e.g. height, weight)
 *
 * All methods are suspend and return Fjuul-style Result<T>.
 *
 * This class is used internally by HealthConnectActivitySource and not exposed directly.
 */
class HealthConnectDataManager(
    private val context: Context,
    private val service: ActivitySourcesService
) {
    private val origins = listOf("healthconnect")

    suspend fun syncIntraday(options: HealthConnectSyncOptions): Result<Unit> {
        val zone = ZoneId.systemDefault()
        var cursor = options.timeRangeStart.atStartOfDay(zone).toInstant()
        val endInstant = options.timeRangeEnd
            .atTime(LocalTime.MAX)
            .atZone(zone)
            .toInstant()

        while (cursor.isBefore(endInstant)) {
            val batchEnd = cursor.plusSeconds(3600).coerceAtMost(endInstant)
            val batchOptions = options.copy(
                timeRangeStart = cursor.atZone(zone).toLocalDate(),
                timeRangeEnd   = batchEnd.atZone(zone).toLocalDate()
            )

            val result = HealthConnectClientWrapper.read(context, batchOptions)
            if (result.isError) {
                return Result.error(result.error ?: Exception("Failed to read intraday data"))
            }
            val points = result.value
                ?: return Result.error(Exception("No data returned from Health Connect"))

            if (options.metrics.contains(FitnessMetricsType.INTRADAY_CALORIES) == true) {
                HealthConnectDataMapper.toCumulativePayload(
                    points,
                    FitnessMetricsType.INTRADAY_CALORIES,
                    origins
                )?.let {
                    val res = service.uploadHealthConnectCumulativeData(it).execute()
                    if (res.isError) {
                        return Result.error(res.error ?: Exception("Failed to upload calories"))
                    }
                }
            }

            if (options.metrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE) == true) {
                HealthConnectDataMapper.toStatisticalPayload(
                    points,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    origins
                )?.let {
                    val res = service.uploadHealthConnectStatisticalData(it).execute()
                    if (res.isError) {
                        return Result.error(res.error ?: Exception("Failed to upload heart rate"))
                    }
                }
            }

            cursor = batchEnd
        }

        return Result.value(Unit)
    }

    suspend fun syncDaily(options: HealthConnectSyncOptions): Result<Unit> {
        val result = HealthConnectClientWrapper.read(context, options)
        if (result.isError) {
            return Result.error(result.error ?: Exception("Failed to read daily data"))
        }
        val points = result.value
            ?: return Result.error(Exception("No daily data returned"))

        val payload = HealthConnectDataMapper.toDailyPayload(
            points,
            options.timeRangeStart.toString(),
            origins
        )
        val res = service.uploadHealthConnectDailies(payload).execute()
        if (res.isError) {
            return Result.error(res.error ?: Exception("Failed to upload daily"))
        }

        return Result.value(Unit)
    }

    suspend fun syncProfile(options: HealthConnectSyncOptions): Result<Boolean> {
        val result = HealthConnectClientWrapper.read(context, options)
        if (result.isError) {
            return Result.error(result.error ?: Exception("Failed to read profile data"))
        }
        val points = result.value
            ?: return Result.error(Exception("No profile data returned"))

        val payload = HealthConnectDataMapper.toProfilePayload(points)
            ?: return Result.value(false)
        val res = service.updateHealthConnectProfile(payload).execute()
        if (res.isError) {
            return Result.error(res.error ?: Exception("Failed to upload profile"))
        }

        return Result.value(true)
    }
}

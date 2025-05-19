package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.entities.Result
import java.time.Instant
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

    /**
     * Synchronizes intraday metrics in 1-hour time batches.
     *
     * Expected metrics: calories, heart rate (via options).
     * For each batch:
     * - Reads data from Health Connect
     * - Aggregates values (sum or min/avg/max)
     * - Uploads payload to the backend
     *
     * @param options configuration that defines which types to sync and time range
     * @return Result.success(Unit) on full success; Result.error(...) on any failure
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions): Result<Unit> {
        var cursor = Instant.ofEpochMilli(options.timeRangeStart)
        val end = Instant.ofEpochMilli(options.timeRangeEnd)

        while (cursor.isBefore(end)) {
            val batchEnd = cursor.plusSeconds(3600).coerceAtMost(end)

            val batchOptions = options.copy(
                timeRangeStart = cursor.toEpochMilli(),
                timeRangeEnd = batchEnd.toEpochMilli()
            )

            val result = HealthConnectClientWrapper.read(context, batchOptions)
            if (result.isError) {
                return Result.error(result.error ?: Exception("Failed to read intraday data"))
            }

            val points = result.value
                ?: return Result.error(Exception("No data returned from Health Connect"))

            if (options.readCalories) {
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

            if (options.readHeartRate) {
                HealthConnectDataMapper.toStatisticalPayload(
                    points,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    origins
                )?.let {
                    val res = service.uploadHealthConnectStatisticalData(it).execute()
                    if (res.isError()) {
                        return Result.error(
                            res.getError() ?: Exception("Failed to upload heart rate")
                        )
                    }
                }
            }

            cursor = batchEnd
        }

        return Result.value(Unit)
    }

    /**
     * Synchronizes daily data for a given day.
     *
     * Expected metrics: total steps, resting heart rate.
     * Time range should fully cover one day (e.g. 00:00 – 23:59).
     *
     * @param options configuration with types and time range
     * @return Result.success(Unit) if data uploaded; Result.error(...) on failure
     */
    suspend fun syncDaily(options: HealthConnectSyncOptions): Result<Unit> {
        val result = HealthConnectClientWrapper.read(context, options)
        if (result.isError) {
            return Result.error(result.error ?: Exception("Failed to read daily data"))
        }

        val points = result.value
            ?: return Result.error(Exception("No daily data returned"))

        val date = Instant.ofEpochMilli(options.timeRangeStart)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val payload = HealthConnectDataMapper.toDailyPayload(points, date.toString(), origins)
        val res = service.uploadHealthConnectDailies(payload).execute()
        if (res.isError) {
            return Result.error(res.error ?: Exception("Failed to upload daily"))
        }

        return Result.value(Unit)
    }

    /**
     * Synchronizes the user's profile data: height and/or weight.
     *
     * Uses the latest known value within the configured time range.
     * If both height and weight are null, no upload is performed.
     *
     * @param options profile sync options
     * @return Result.success(true) if profile uploaded, false if nothing to upload
     *         Result.error(...) on failure
     */
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

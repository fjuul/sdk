package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.core.entities.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * A low-level wrapper around Android's HealthConnectClient.
 *
 * Provides a single entry point to read Health Connect data records based on the provided sync options.
 * The output is a unified list of HealthConnectDataPoint, each tagged with its corresponding FitnessMetricsType.
 *
 * This abstraction allows the upper layers (DataManager, ActivitySource) to remain decoupled from Android APIs.
 *
 * Use-case:
 * val result = HealthConnectClientWrapper.read(context, options)
 */

object HealthConnectClientWrapper {

    suspend fun read(
        context: Context,
        options: HealthConnectSyncOptions
    ): Result<List<HealthConnectDataPoint>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = HealthConnectClient.getOrCreate(context)
            val start = Instant.ofEpochMilli(options.timeRangeStart)
            val end = Instant.ofEpochMilli(options.timeRangeEnd)
            val timeFilter = TimeRangeFilter.between(start, end)

            val result = mutableListOf<HealthConnectDataPoint>()

            if (options.readSteps) {
                val steps = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeFilter)
                ).records
                result += steps.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.INTRADAY_STEPS,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        value = it.count.toDouble()
                    )
                }
            }

            if (options.readCalories) {
                val cals = client.readRecords(
                    ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeFilter)
                ).records
                result += cals.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.INTRADAY_CALORIES,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        value = it.energy.inKilocalories
                    )
                }
            }

            if (options.readHeartRate) {
                val hrs = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeFilter)
                ).records
                result += hrs.flatMap { record ->
                    record.samples.map {
                        HealthConnectDataPoint(
                            type = FitnessMetricsType.INTRADAY_HEART_RATE,
                            startTime = it.time,
                            endTime = it.time,
                            value = it.beatsPerMinute.toDouble()
                        )
                    }
                }
            }

            if (options.readHeight) {
                val heights = client.readRecords(
                    ReadRecordsRequest(HeightRecord::class, timeFilter)
                ).records
                result += heights.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.HEIGHT,
                        startTime = it.time,
                        endTime = it.time,
                        value = it.height.inMeters
                    )
                }
            }

            if (options.readWeight) {
                val weights = client.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeFilter)
                ).records
                result += weights.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.WEIGHT,
                        startTime = it.time,
                        endTime = it.time,
                        value = it.weight.inKilograms
                    )
                }
            }

            Result.value(result)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}


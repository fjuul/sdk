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
 * Low-level access to Android Health Connect API.
 * Translates raw records into internal HealthConnectDataPoint format.
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
                val records = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeFilter)).records
                result += records.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.INTRADAY_STEPS,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        value = it.count.toDouble()
                    )
                }
            }

            if (options.readCalories) {
                val records = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeFilter)).records
                result += records.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.INTRADAY_CALORIES,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        value = it.energy.inKilocalories
                    )
                }
            }

            if (options.readHeartRate) {
                val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeFilter)).records
                result += records.flatMap { record ->
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
                val records = client.readRecords(ReadRecordsRequest(HeightRecord::class, timeFilter)).records
                result += records.map {
                    HealthConnectDataPoint(
                        type = FitnessMetricsType.HEIGHT,
                        startTime = it.time,
                        endTime = it.time,
                        value = it.height.inMeters
                    )
                }
            }

            if (options.readWeight) {
                val records = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeFilter)).records
                result += records.map {
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

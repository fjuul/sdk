package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.core.entities.Result
import java.time.LocalTime
import java.time.ZoneId

/**
 * Low-level access to Android Health Connect API.
 * Translates raw records into internal HealthConnectDataPoint format.
 */
object HealthConnectClientWrapper {

    suspend fun read(
        context: Context,
        options: HealthConnectSyncOptions
    ): Result<List<HealthConnectDataPoint>>{
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val zone = ZoneId.systemDefault()
            val start = options.timeRangeStart
                .atStartOfDay(zone)
                .toInstant()
            val end = options.timeRangeEnd
                .atTime(LocalTime.MAX)
                .atZone(zone)
                .toInstant()
            val timeFilter = TimeRangeFilter.between(start, end)

            val result = mutableListOf<HealthConnectDataPoint>()

            if (options.readSteps == true) {
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

            if (options.readCalories == true) {
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

            if (options.readHeartRate == true) {
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

            if (options.readHeight == true) {
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

            if (options.readWeight == true) {
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

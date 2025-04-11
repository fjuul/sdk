package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.CommonException
import java.time.Instant
import java.util.Date
import androidx.health.connect.client.HealthConnectClient as HealthConnectApiClient

class HealthConnectClient(private val context: Context) {
    private val healthConnectClient: HealthConnectApiClient by lazy {
        HealthConnectApiClient.getOrCreate(context)
    }

    suspend fun hasAllPermissions(): Boolean {
        try {
            val permissions = setOf(
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(WeightRecord::class),
                HealthPermission.getReadPermission(HeightRecord::class)
            )
            return healthConnectClient.permissionController.getGrantedPermissions()
                .containsAll(permissions)
        } catch (e: Exception) {
            throw CommonException("Failed to check Health Connect permissions: ${e.message}")
        }
    }

    suspend fun readSteps(startTime: Long, endTime: Long): List<StepsDataPoint> {
        try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            return response.records.map { record ->
                StepsDataPoint(
                    startTime = Date(record.startTime.toEpochMilli()),
                    endTime = Date(record.endTime.toEpochMilli()),
                    dataSource = record.metadata.dataOrigin.packageName,
                    count = record.count
                )
            }
        } catch (e: Exception) {
            throw CommonException("Failed to read steps from Health Connect: ${e.message}")
        }
    }

    suspend fun readHeartRate(startTime: Long, endTime: Long): List<HeartRateDataPoint> {
        try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            return response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateDataPoint(
                        startTime = Date(sample.time.toEpochMilli()),
                        endTime = Date(sample.time.toEpochMilli()),
                        dataSource = record.metadata.dataOrigin.packageName,
                        beatsPerMinute = sample.beatsPerMinute.toLong()
                    )
                }
            }
        } catch (e: Exception) {
            throw CommonException("Failed to read heart rate from Health Connect: ${e.message}")
        }
    }

    suspend fun readCalories(startTime: Long, endTime: Long): List<CaloriesDataPoint> {
        try {
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            return response.records.map { record ->
                CaloriesDataPoint(
                    startTime = Date(record.startTime.toEpochMilli()),
                    endTime = Date(record.endTime.toEpochMilli()),
                    dataSource = record.metadata.dataOrigin.packageName,
                    calories = record.energy.inKilocalories
                )
            }
        } catch (e: Exception) {
            throw CommonException("Failed to read calories from Health Connect: ${e.message}")
        }
    }

    suspend fun readProfileData(): HealthConnectProfileData {
        try {
            val now = Instant.now()
            val startTime = now.minusSeconds(24 * 60 * 60) // 24 hours ago

            val weightRequest = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, now)
            )
            val heightRequest = ReadRecordsRequest(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, now)
            )

            val weightResponse = healthConnectClient.readRecords(weightRequest)
            val heightResponse = healthConnectClient.readRecords(heightRequest)

            val latestWeight = weightResponse.records.maxByOrNull { it.time }?.weight?.inKilograms
            val latestHeight = heightResponse.records.maxByOrNull { it.time }?.height?.inMeters

            return HealthConnectProfileData(
                weight = latestWeight,
                height = latestHeight
            )
        } catch (e: Exception) {
            throw CommonException("Failed to read profile data from Health Connect: ${e.message}")
        }
    }
}

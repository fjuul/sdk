package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectClientWrapper(private val context: Context) {
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun hasAllPermissions(): Boolean {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class)
        )
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readSteps(startTime: Long, endTime: Long): List<HealthConnectStepsData> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.ofEpochMilli(startTime),
                Instant.ofEpochMilli(endTime)
            )
        )
        val response = healthConnectClient.readRecords(request)
        return response.records.map { record ->
            HealthConnectStepsData(
                count = record.count,
                startTime = record.startTime.toEpochMilli(),
                endTime = record.endTime.toEpochMilli()
            )
        }
    }

    suspend fun readHeartRate(startTime: Long, endTime: Long): List<HealthConnectHeartRateData> {
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
                HealthConnectHeartRateData(
                    beatsPerMinute = sample.beatsPerMinute.toLong(),
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli()
                )
            }
        }
    }

    suspend fun readCalories(startTime: Long, endTime: Long): List<HealthConnectCaloriesData> {
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.ofEpochMilli(startTime),
                Instant.ofEpochMilli(endTime)
            )
        )
        val response = healthConnectClient.readRecords(request)
        return response.records.map { record ->
            HealthConnectCaloriesData(
                calories = record.energy.inKilocalories,
                startTime = record.startTime.toEpochMilli(),
                endTime = record.endTime.toEpochMilli()
            )
        }
    }

    suspend fun readProfileData(): HealthConnectProfileData {
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

        return HealthConnectProfileData(
            weight = weightResponse.records.maxByOrNull { it.time }?.weight?.inKilograms,
            height = heightResponse.records.maxByOrNull { it.time }?.height?.inMeters
        )
    }
}

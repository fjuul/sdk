package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.CommonException
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.HealthConnectPermissionsNotGrantedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Date

/**
 * Client for interacting with the Health Connect API.
 * Handles data reading and transformation.
 *
 * This class provides methods to read various health metrics from Health Connect:
 * - Steps
 * - Heart Rate
 * - Calories
 * - Profile data (weight and height)
 */
class HealthConnectClient(
    private val context: Context,
    private val permissionManager: HealthConnectPermissionManager
) {
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Reads steps data for the given time range.
     *
     * @param startTime Start time in milliseconds since epoch
     * @param endTime End time in milliseconds since epoch
     * @return List of steps data points
     * @throws HealthConnectPermissionsNotGrantedException if steps permission is not granted
     * @throws CommonException if there's an error reading steps
     */
    suspend fun readSteps(startTime: Long, endTime: Long): List<StepsDataPoint> = withContext(Dispatchers.IO) {
        try {
            permissionManager.ensurePermissions()

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                StepsDataPoint(
                    startTime = Date(record.startTime.toEpochMilli()),
                    endTime = Date(record.endTime.toEpochMilli()),
                    dataSource = record.metadata.dataOrigin.packageName,
                    count = record.count
                )
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            Log.e(TAG, "Failed to read steps: permissions not granted", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps from Health Connect", e)
            throw CommonException("Failed to read steps from Health Connect: ${e.message}")
        }
    }

    /**
     * Reads heart rate data for the given time range.
     *
     * @param startTime Start time in milliseconds since epoch
     * @param endTime End time in milliseconds since epoch
     * @return List of heart rate data points
     * @throws HealthConnectPermissionsNotGrantedException if heart rate permission is not granted
     * @throws CommonException if there's an error reading heart rate
     */
    suspend fun readHeartRate(startTime: Long, endTime: Long): List<HeartRateDataPoint> = withContext(Dispatchers.IO) {
        try {
            permissionManager.ensurePermissions()

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateDataPoint(
                        startTime = Date(sample.time.toEpochMilli()),
                        endTime = Date(sample.time.toEpochMilli()),
                        dataSource = record.metadata.dataOrigin.packageName,
                        beatsPerMinute = sample.beatsPerMinute.toLong()
                    )
                }
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            Log.e(TAG, "Failed to read heart rate: permissions not granted", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate from Health Connect", e)
            throw CommonException("Failed to read heart rate from Health Connect: ${e.message}")
        }
    }

    /**
     * Reads calories data for the given time range.
     *
     * @param startTime Start time in milliseconds since epoch
     * @param endTime End time in milliseconds since epoch
     * @return List of calories data points
     * @throws HealthConnectPermissionsNotGrantedException if calories permission is not granted
     * @throws CommonException if there's an error reading calories
     */
    suspend fun readCalories(startTime: Long, endTime: Long): List<CaloriesDataPoint> = withContext(Dispatchers.IO) {
        try {
            permissionManager.ensurePermissions()

            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime)
                )
            )
            val response = healthConnectClient.readRecords(request)
            response.records.map { record ->
                CaloriesDataPoint(
                    startTime = Date(record.startTime.toEpochMilli()),
                    endTime = Date(record.endTime.toEpochMilli()),
                    dataSource = record.metadata.dataOrigin.packageName,
                    calories = record.energy.inKilocalories
                )
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            Log.e(TAG, "Failed to read calories: permissions not granted", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read calories from Health Connect", e)
            throw CommonException("Failed to read calories from Health Connect: ${e.message}")
        }
    }

    /**
     * Reads profile data (weight and height).
     *
     * @return Profile data with weight and height
     * @throws HealthConnectPermissionsNotGrantedException if profile permissions are not granted
     * @throws CommonException if there's an error reading profile data
     */
    suspend fun readProfileData(): HealthConnectProfileData = withContext(Dispatchers.IO) {
        try {
            permissionManager.ensurePermissions()

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

            HealthConnectProfileData(
                weight = latestWeight,
                height = latestHeight
            )
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            Log.e(TAG, "Failed to read profile data: permissions not granted", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read profile data from Health Connect", e)
            throw CommonException("Failed to read profile data from Health Connect: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "HealthConnectClient"
    }
}

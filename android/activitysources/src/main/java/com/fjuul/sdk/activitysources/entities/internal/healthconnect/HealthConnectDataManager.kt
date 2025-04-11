package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.CommonException
import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.HealthConnectPermissionsNotGrantedException
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.utils.Logger
import java.util.Date

/**
 * Manages Health Connect data operations including reading and uploading health data.
 * This class handles three main types of data:
 * 1. Intraday data (steps, heart rate, calories)
 * 2. Daily summaries (steps, resting heart rate)
 * 3. Profile data (weight, height)
 */
class HealthConnectDataManager(
    private val context: Context,
    private val activitySourcesService: ActivitySourcesService
) {
    private val permissionManager = HealthConnectPermissionManager(context)
    private val client = HealthConnectClient(context, permissionManager)
    private val logger = Logger.get()

    /**
     * Reads and uploads intraday data for a specific time range.
     * This includes steps, heart rate, and calories data points.
     */
    suspend fun uploadIntradayData(startTime: Long, endTime: Long) {
        try {
            permissionManager.ensurePermissions()

            val steps = client.readSteps(startTime, endTime)
            val heartRate = client.readHeartRate(startTime, endTime)
            val calories = client.readCalories(startTime, endTime)

            val sources = (steps.mapNotNull { it.dataSource } +
                heartRate.mapNotNull { it.dataSource } +
                calories.mapNotNull { it.dataSource }).toSet()

            val entries = buildList {
                addAll(steps.map {
                    HealthConnectIntradayEntry(
                        start = it.startTime,
                        value = it.count.toDouble()
                    )
                })
                addAll(heartRate.map {
                    HealthConnectIntradayEntry(
                        start = it.startTime,
                        min = it.beatsPerMinute.toDouble(),
                        avg = it.beatsPerMinute.toDouble(),
                        max = it.beatsPerMinute.toDouble()
                    )
                })
                addAll(calories.map {
                    HealthConnectIntradayEntry(
                        start = it.startTime,
                        value = it.calories
                    )
                })
            }

            if (entries.isNotEmpty()) {
                activitySourcesService.uploadHealthConnectData(
                    HealthConnectIntradayData(
                        dataOrigins = sources.toList(),
                        entries = entries
                    )
                )
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            throw e
        } catch (e: Exception) {
            throw CommonException("Failed to upload intraday data: ${e.message}")
        }
    }

    /**
     * Reads and uploads daily data for a specific date.
     * This includes steps and resting heart rate data.
     */
    suspend fun uploadDailyData(date: Date) {
        try {
            permissionManager.ensurePermissions()

            val startTime = date.time
            val endTime = startTime + 24 * 60 * 60 * 1000

            val steps = client.readSteps(startTime, endTime)
            val heartRate = client.readHeartRate(startTime, endTime)

            if (steps.isEmpty() && heartRate.isEmpty()) return

            val sources = (steps.mapNotNull { it.dataSource } +
                heartRate.mapNotNull { it.dataSource }).toSet()

            val dailySteps = if (steps.isNotEmpty()) steps.sumOf { it.count }.toInt() else null
            val restingHeartRate = if (heartRate.isNotEmpty()) {
                val rates = heartRate.map { it.beatsPerMinute.toDouble() }
                RestingHeartRate(
                    min = rates.min(),
                    avg = rates.average(),
                    max = rates.max()
                )
            } else null

            if (dailySteps != null || restingHeartRate != null) {
                activitySourcesService.uploadHealthConnectDailies(
                    HealthConnectDailiesData(
                        date = date,
                        dataOrigins = sources.toList(),
                        steps = dailySteps,
                        restingHeartRate = restingHeartRate
                    )
                )
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            throw e
        } catch (e: Exception) {
            throw CommonException("Failed to upload daily data: ${e.message}")
        }
    }

    /**
     * Reads and uploads profile data including weight and height.
     */
    suspend fun uploadProfileData() {
        try {
            permissionManager.ensurePermissions()
            val profile = client.readProfileData()
            if (profile.weight != null || profile.height != null) {
                activitySourcesService.updateHealthConnectProfile(profile)
            }
        } catch (e: HealthConnectPermissionsNotGrantedException) {
            throw e
        } catch (e: Exception) {
            throw CommonException("Failed to upload profile data: ${e.message}")
        }
    }
}

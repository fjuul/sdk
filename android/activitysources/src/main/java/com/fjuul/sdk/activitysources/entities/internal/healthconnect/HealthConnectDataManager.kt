package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.exceptions.HealthConnectActivitySourceExceptions.CommonException
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Manages the synchronization of Health Connect data with our backend.
 * Handles data reading, transformation, and uploading.
 */
class HealthConnectDataManager(
    private val client: HealthConnectClient,
    private val sourcesService: ActivitySourcesService
) {
    private val logger = Logger.get()

    /**
     * Synchronizes intraday metrics (steps, heart rate, calories) with our backend.
     * @param startTime The start time of the sync period
     * @param endTime The end time of the sync period
     */
    suspend fun syncIntradayMetrics(startTime: Long, endTime: Long) =
        withContext(Dispatchers.IO) {
            logger.d("Starting Health Connect intraday sync from $startTime to $endTime")

            try {
                val dataOrigins = mutableSetOf<String>()
                val entries = mutableListOf<HealthConnectIntradayEntry>()

                // Read and process calories
                val caloriesData = client.readCalories(startTime, endTime)
                dataOrigins.addAll(caloriesData.mapNotNull { it.dataSource })
                entries.addAll(caloriesData.map {
                    HealthConnectIntradayEntry(
                        start = it.startTime,
                        value = it.calories
                    )
                })

                // Read and process heart rate
                val heartRateData = client.readHeartRate(startTime, endTime)
                dataOrigins.addAll(heartRateData.mapNotNull { it.dataSource })
                entries.addAll(heartRateData.map {
                    HealthConnectIntradayEntry(
                        start = it.startTime,
                        min = it.beatsPerMinute.toDouble(),
                        avg = it.beatsPerMinute.toDouble(),
                        max = it.beatsPerMinute.toDouble()
                    )
                })

                if (entries.isNotEmpty()) {
                    val intradayData = HealthConnectIntradayData(
                        dataOrigins = dataOrigins.toList(),
                        entries = entries
                    )
                    sourcesService.uploadHealthConnectData(intradayData)
                }

                logger.d("Health Connect intraday sync completed successfully")
            } catch (e: Exception) {
                throw CommonException("Failed to sync intraday metrics: ${e.message}")
            }
        }

    suspend fun syncDailyMetrics(date: Date) =
        withContext(Dispatchers.IO) {
            logger.d("Starting Health Connect daily sync for date: $date")

            try {
                val dataOrigins = mutableSetOf<String>()
                var steps: Int? = null
                var restingHeartRate: RestingHeartRate? = null

                // Read and process steps
                val stepsData = client.readSteps(date.time, date.time + 86400000) // 24 hours
                if (stepsData.isNotEmpty()) {
                    dataOrigins.addAll(stepsData.mapNotNull { it.dataSource })
                    steps = stepsData.sumOf { it.count }.toInt()
                }

                // Read and process heart rate for resting calculation
                val heartRateData = client.readHeartRate(date.time, date.time + 86400000)
                if (heartRateData.isNotEmpty()) {
                    dataOrigins.addAll(heartRateData.mapNotNull { it.dataSource })
                    val heartRates = heartRateData.map { it.beatsPerMinute.toDouble() }
                    if (heartRates.isNotEmpty()) {
                        restingHeartRate = RestingHeartRate(
                            min = heartRates.min(),
                            avg = heartRates.average(),
                            max = heartRates.max()
                        )
                    }
                }

                if (steps != null || restingHeartRate != null) {
                    val dailiesData = HealthConnectDailiesData(
                        date = date,
                        dataOrigins = dataOrigins.toList(),
                        steps = steps,
                        restingHeartRate = restingHeartRate
                    )
                    sourcesService.uploadHealthConnectDailies(dailiesData)
                }

                logger.d("Health Connect daily sync completed successfully")
            } catch (e: Exception) {
                throw CommonException("Failed to sync daily metrics: ${e.message}")
            }
        }

    /**
     * Synchronizes profile data (weight, height) with our backend.
     */
    suspend fun syncProfileData() =
        withContext(Dispatchers.IO) {
            logger.d("Starting Health Connect profile sync")

            try {
                val profileData = client.readProfileData()
                if (profileData.weight != null || profileData.height != null) {
                    sourcesService.updateHealthConnectProfile(profileData)
                }

                logger.d("Health Connect profile sync completed successfully")
            } catch (e: Exception) {
                throw CommonException("Failed to sync profile data: ${e.message}")
            }
        }
}

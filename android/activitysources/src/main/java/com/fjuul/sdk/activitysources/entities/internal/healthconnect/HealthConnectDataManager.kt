package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.activitysources.utils.roundTo
import com.fjuul.sdk.core.entities.IStorage
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Handles synchronization of Health Connect data:
 * 1. Intraday: per-minute cumulative (calories) and statistical (heart rate) buckets, uploaded per day.
 * 2. Daily: aggregated daily steps and resting heart rate.
 * 3. Profile: latest height (cm) and weight (kg).
 */
class HealthConnectDataManager(
    private val client: HealthConnectClient,
    private val service: ActivitySourcesService,
    private val storage: IStorage,
) {

    companion object {
        private const val HEART_RATE_CHANGES_TOKEN = "HEART_RATE_CHANGES_TOKEN"
        private const val TOTAL_CALORIES_CHANGES_TOKEN = "TOTAL_CALORIES_CHANGES_TOKEN"
        private const val ACTIVE_CALORIES_CHANGES_TOKEN = "ACTIVE_CALORIES_CHANGES_TOKEN"
    }

    /**
     * Synchronize intraday data: fetches 1-minute buckets over the last 2 days for all requested metrics,
     * groups them by local date, and uploads each day as a separate payload.
     *
     * @throws HealthConnectException.NoMetricsSelectedException if no metrics are selected or on upload error.
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        var heartRateTimeChanges = listOf<Instant>()
        if (options.metrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE)) {
            var heartRateChangesToken = storage.get(HEART_RATE_CHANGES_TOKEN)

            if (heartRateChangesToken.isNullOrEmpty()) {
                heartRateChangesToken = client.getChangesToken(
                    ChangesTokenRequest(recordTypes = setOf(HeartRateRecord::class))
                )
            }

            // return if heart rate changes token is empty
            if (heartRateChangesToken.isEmpty()) return

            heartRateTimeChanges = getTimeChangesList(heartRateChangesToken) {
                storage.set(HEART_RATE_CHANGES_TOKEN, heartRateChangesToken)
            }
        }

        var caloriesTimeChanges = mutableListOf<Instant>()
        if (options.metrics.contains(FitnessMetricsType.INTRADAY_CALORIES)) {
            var activeCaloriesChangesToken = storage.get(ACTIVE_CALORIES_CHANGES_TOKEN)
            var totalCaloriesChangesToken = storage.get(TOTAL_CALORIES_CHANGES_TOKEN)

            if (activeCaloriesChangesToken.isNullOrEmpty()) {
                activeCaloriesChangesToken = client.getChangesToken(
                    ChangesTokenRequest(recordTypes = setOf(ActiveCaloriesBurnedRecord::class))
                )
            }

            if (totalCaloriesChangesToken.isNullOrEmpty()) {
                totalCaloriesChangesToken = client.getChangesToken(
                    ChangesTokenRequest(recordTypes = setOf(TotalCaloriesBurnedRecord::class))
                )
            }

            if (activeCaloriesChangesToken.isNotEmpty()) {
                caloriesTimeChanges = getTimeChangesList(activeCaloriesChangesToken) {
                    storage.set(ACTIVE_CALORIES_CHANGES_TOKEN, activeCaloriesChangesToken)
                }
            }

            if (totalCaloriesChangesToken.isNotEmpty()) {
                caloriesTimeChanges.addAll(getTimeChangesList(totalCaloriesChangesToken) {
                    storage.set(TOTAL_CALORIES_CHANGES_TOKEN, totalCaloriesChangesToken)
                })
            }
        }

        if (heartRateTimeChanges.isNotEmpty()) {
            val heartRateMetric =
                setOf(FitnessMetricsType.INTRADAY_HEART_RATE).flatMap { it.toAggregateMetrics() }
                    .toSet()
            syncChangedBuckets(heartRateMetric, heartRateTimeChanges)
        }

        if (caloriesTimeChanges.isNotEmpty()) {
            val caloriesRateMetric =
                setOf(FitnessMetricsType.INTRADAY_CALORIES).flatMap { it.toAggregateMetrics() }
                    .toSet()
            syncChangedBuckets(caloriesRateMetric, caloriesTimeChanges)
        }
    }

    private suspend fun getTimeChangesList(
        token: String,
        onTokenSave: (String) -> Unit
    ): MutableList<Instant> {
        val timeChangesList = mutableListOf<Instant>()
        var nextChangesToken = token
        do {
            val response = client.getChanges(nextChangesToken)
            response.changes.forEach { change ->
                when (change) {
                    is UpsertionChange -> {
                        timeChangesList.add(change.record.metadata.lastModifiedTime)
                    }
                }
            }
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)

        onTokenSave(nextChangesToken)

        return timeChangesList
    }

    private suspend fun syncChangedBuckets(
        metrics: Set<AggregateMetric<*>>,
        timeChanges: List<Instant>
    ) {
        val start = timeChanges.sorted()[0]
        val zone = ZoneOffset.UTC
        val now = Instant.now()
        // Read 1-minute buckets
        val buckets: List<AggregationResultGroupedByDuration> = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(start, now),
                timeRangeSlicer = Duration.ofMinutes(1)
            )
        )
        if (buckets.isEmpty()) return

        val changedBuckets = mutableListOf<AggregationResultGroupedByDuration>()
        timeChanges
            .sorted()
            .forEach { time ->
                buckets.firstOrNull { (it.startTime > time && it.endTime < time) || it.startTime == time || it.endTime == time }
                    ?.let { changedBucket ->
                        changedBuckets.add(changedBucket)
                    }
            }


        // Group by date and upload
        changedBuckets
            .toSet()
            .sortedBy { it.startTime }
            .groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
            .forEach { (_, dayBuckets) ->
                // Collect all distinct data origins for this day
                val origins = dayBuckets
                    .flatMap { it.result.dataOrigins.map { od -> od.packageName } }
                    .distinct()

                // Build cumulative calorie entries
                val totalEntries = dayBuckets.mapNotNull { b ->
                    b.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                        ?.inKilocalories
                        ?.roundTo(2)
                        ?.let { ValueEntry(b.startTime.toString(), it) }
                }
                val activeEntries = dayBuckets.mapNotNull { bucket ->
                    bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                        ?.inKilocalories
                        ?.roundTo(2)
                        ?.let { ValueEntry(bucket.startTime.toString(), it) }
                }

                // Build statistical heart-rate entries
                val hrEntries = dayBuckets.mapNotNull { b ->
                    val min = b.result[HeartRateRecord.BPM_MIN]?.toDouble()
                    val avg = b.result[HeartRateRecord.BPM_AVG]?.toDouble()
                    val max = b.result[HeartRateRecord.BPM_MAX]?.toDouble()
                    if (min != null || avg != null || max != null) {
                        HeartRateEntry(
                            start = b.startTime.toString(),
                            min = min ?: 0.0,
                            avg = avg ?: 0.0,
                            max = max ?: 0.0
                        )
                    } else null
                }

                // Skip if nothing to send
                if (totalEntries.isEmpty() && activeEntries.isEmpty() && hrEntries.isEmpty()) return@forEach

                // Assemble payload
                val payload = HealthConnectIntradayPayload(
                    totalCalories = totalEntries.takeIf { it.isNotEmpty() }
                        ?.let { MetricData(origins, it) },
                    activeCalories = activeEntries.takeIf { it.isNotEmpty() }
                        ?.let { MetricData(origins, it) },
                    heartrate = hrEntries.takeIf { it.isNotEmpty() }
                        ?.let { MetricData(origins, it) }
                )

                // Upload and check for errors
                service.uploadHealthConnectIntraday(payload)
                    .execute()
                    .let { if (it.isError) throw it.error!! }
            }
    }

    /**
     * Synchronize daily summary data: fetch today’s steps and resting heart rate,
     * build payload entries for each day, and upload if there is any data.
     *
     * @param options contains the set of metrics to sync
     * @throws HealthConnectException if no metrics are selected or upload fails
     */
    suspend fun syncDaily(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val metrics = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()

        // Define the time window for today
        val todayStart = LocalDate.now().atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)

        // Request daily aggregates (1-day buckets) from Health Connect
        val buckets: List<AggregationResultGroupedByPeriod> = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(todayStart, tomorrowStart),
                timeRangeSlicer = Period.ofDays(1)
            )
        )

        // Transform each non-empty bucket into a DailyEntry
        val entries = buckets.mapNotNull { b ->
            // Extract step count, if available
            val steps = b.result[StepsRecord.COUNT_TOTAL]

            // Extract resting heart rate stats, if any values present
            val minHr = b.result[RestingHeartRateRecord.BPM_MIN]?.toDouble()
            val avgHr = b.result[RestingHeartRateRecord.BPM_AVG]?.toDouble()
            val maxHr = b.result[RestingHeartRateRecord.BPM_MAX]?.toDouble()
            val hrEntry = if (minHr != null || avgHr != null || maxHr != null) {
                HeartRateEntry(start = null, min = minHr, avg = avgHr, max = maxHr)
            } else null

            if (steps == null && hrEntry == null) return@mapNotNull null

            DailyEntry(
                date = b.startTime.toLocalDate().toString(),
                dataOrigins = b.result.dataOrigins.map { it.packageName },
                steps = steps,
                restingHeartRate = hrEntry
            )
        }

        if (entries.isEmpty()) return

        service.uploadHealthConnectDailies(HealthConnectDailiesPayload(entries))
            .execute()
            .let { if (it.isError) throw it.error!! }
    }

    /**
     * Synchronize profile data: reads up to 30 days of height and weight records,
     * picks the most recent values and uploads them.
     * @throws HealthConnectException.NoMetricsSelectedException if no metrics are selected or on upload error.
     */
    suspend fun syncProfile(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

        // Read height and weight
        val heightsResp = if (options.metrics.contains(FitnessMetricsType.HEIGHT)) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(thirtyDaysAgo, now)
                )
            )
        } else null

        val weightsResp = if (options.metrics.contains(FitnessMetricsType.WEIGHT)) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(thirtyDaysAgo, now)
                )
            )
        } else null

        // Pick latest measurements
        val latestHeightCm = heightsResp
            ?.records
            ?.maxByOrNull { it.time }
            ?.height
            ?.inMeters
            ?.times(100.0)
            ?.roundTo(2)

        val latestWeightKg = weightsResp
            ?.records
            ?.maxByOrNull { it.time }
            ?.weight
            ?.inKilograms
            ?.roundTo(2)

        if (latestHeightCm == null && latestWeightKg == null) return

        service.uploadHealthConnectProfile(
            HealthConnectProfilePayload(height = latestHeightCm, weight = latestWeightKg)
        )
            .execute()
            .let { if (it.isError) throw it.error!! }
    }
}

/**
 * Extension to map fitness metrics enum to Health Connect AggregateMetric.
 */
private fun FitnessMetricsType.toAggregateMetrics(): Set<AggregateMetric<*>> = when (this) {
    FitnessMetricsType.INTRADAY_CALORIES -> setOf(
        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
    )

    FitnessMetricsType.INTRADAY_HEART_RATE -> setOf(
        HeartRateRecord.BPM_MIN,
        HeartRateRecord.BPM_AVG,
        HeartRateRecord.BPM_MAX
    )

    FitnessMetricsType.RESTING_HEART_RATE -> setOf(
        RestingHeartRateRecord.BPM_MIN,
        RestingHeartRateRecord.BPM_AVG,
        RestingHeartRateRecord.BPM_MAX
    )

    FitnessMetricsType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)
    else -> throw HealthConnectException.UnsupportedMetricException(this.name)
}

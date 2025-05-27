package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.activitysources.utils.roundTo
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthConnectDataManager(
    private val client: HealthConnectClient, private val service: ActivitySourcesService
) {
    /** Synchronize hourly intraday aggregates, split per day. */
    suspend fun syncIntraday(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val metricsSet = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()
        val zone = ZoneOffset.UTC
        // Define 3-day window: from 3 days ago at start of day (UTC) until now
        val nowInstant = Instant.now()
        val startInstant = LocalDate.now().minusDays(3).atStartOfDay().toInstant(zone)

        // fetch minute-level buckets for today
        val allBuckets: List<AggregationResultGroupedByDuration> = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = metricsSet,
                timeRangeFilter = TimeRangeFilter.between(startInstant, nowInstant),
                timeRangeSlicer = Duration.ofMinutes(1)
            )
        )

        // map each bucket → IntradayEntry with ISO timestamp
        val allEntries = allBuckets.map { bucket ->
            val map = mutableMapOf<String, Double>()
            bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.let { e: Energy ->
                map["caloriesTotal"] = e.inKilocalories
            }
            bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let { e: Energy ->
                map["activeCalories"] = e.inKilocalories
            }
            bucket.result[HeartRateRecord.BPM_MIN]?.let {
                map["heartRateMin"] = it.toDouble()
            }
            bucket.result[HeartRateRecord.BPM_AVG]?.let {
                map["heartRateAvg"] = it.toDouble()
            }
            bucket.result[HeartRateRecord.BPM_MAX]?.let {
                map["heartRateMax"] = it.toDouble()
            }

            IntradayEntry(
                start = bucket.startTime.toString(), // ISO 8601
                dataOrigins = bucket.result.dataOrigins.map { it.packageName }, metrics = map
            )
        }

        // group by local date and upload one batch per day
        allEntries.groupBy { it.start.substring(0, 10) /* "YYYY-MM-DD" */ }
            .forEach { (_, entriesForDate) ->
                val result =
                    service.uploadHealthConnectIntraday(HealthConnectIntradayPayload(entriesForDate))
                        .execute()

                if (result.isError) {
                    throw result.error!!
                }
            }
    }

    /** Synchronize daily summary data. */
    suspend fun syncDaily(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val metricsSet = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()

        // today's bounds in local time
        val todayStart = LocalDate.now().atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)

        val raw: List<AggregationResultGroupedByPeriod> = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = metricsSet,
                timeRangeFilter = TimeRangeFilter.between(todayStart, tomorrowStart),
                timeRangeSlicer = Period.ofDays(1)
            )
        )

        val entries = raw.map { bucket ->
            val minHr = bucket.result[RestingHeartRateRecord.BPM_MIN]
            val avgHr = bucket.result[RestingHeartRateRecord.BPM_AVG]
            val maxHr = bucket.result[RestingHeartRateRecord.BPM_MAX]
            val stat = if (minHr != null || avgHr != null || maxHr != null) {
                StatisticalAggregateValue(
                    min = minHr?.toDouble(), avg = avgHr?.toDouble(), max = maxHr?.toDouble()
                )
            } else null

            DailyEntry(
                date = bucket.startTime.toLocalDate().toString(),
                dataOrigins = bucket.result.dataOrigins.map { it.packageName },
                steps = bucket.result[StepsRecord.COUNT_TOTAL],
                restingHeartRate = stat
            )
        }

        val result =
            service.uploadHealthConnectDailies(HealthConnectDailiesPayload(entries)).execute()

        if (result.isError) {
            throw result.error!!
        }
    }

    /** Sync only the latest height (in cm) & weight (in kg). */
    suspend fun syncProfile(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        // read all available records (Health Connect default history window)
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)
        var heightsResp: ReadRecordsResponse<HeightRecord>? = null
        var weightsResp: ReadRecordsResponse<WeightRecord>? = null

        // read all height records
        if (options.metrics.contains(FitnessMetricsType.HEIGHT)) {
            heightsResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(thirtyDaysAgo, now)
                )
            )
        }
        // read all weight records
        if (options.metrics.contains(FitnessMetricsType.WEIGHT)) {
            weightsResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(thirtyDaysAgo, now)
                )
            )
        }

        // pick most recent height in meters → convert to cm
        val latestHeightCm: Double? =
            heightsResp?.records?.maxByOrNull { it.time }?.height?.inMeters?.times(100.0)
                ?.roundTo(2)

        // pick most recent weight in kg
        val latestWeightKg: Double? =
            weightsResp?.records?.maxByOrNull { it.time }?.weight?.inKilograms?.roundTo(2)

        if (latestHeightCm == null && latestWeightKg == null) return

        val result = service.uploadHealthConnectProfile(
            HealthConnectProfilePayload(
                height = latestHeightCm, weight = latestWeightKg
            )
        ).execute()

        if (result.isError) {
            throw result.error!!
        }
    }
}

/** Map our enum to Health Connect aggregate metrics. */
fun FitnessMetricsType.toAggregateMetrics(): Set<AggregateMetric<*>> = when (this) {
    FitnessMetricsType.INTRADAY_CALORIES -> setOf(
        TotalCaloriesBurnedRecord.ENERGY_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
    )

    FitnessMetricsType.INTRADAY_HEART_RATE -> setOf(
        HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX
    )

    FitnessMetricsType.RESTING_HEART_RATE -> setOf(
        RestingHeartRateRecord.BPM_MIN,
        RestingHeartRateRecord.BPM_AVG,
        RestingHeartRateRecord.BPM_MAX
    )

    FitnessMetricsType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)

    else -> throw HealthConnectException.UnsupportedMetricException(this.name)
}

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
    /**
     * Synchronize intraday data aggregated into 1-minute buckets,
     * then upload per day with separate "cumulative" and "statistical" sections.
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        // map fitness metric types to Health Connect metrics
        val metricsSet = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()
        val zone = ZoneOffset.UTC

        // define window: from 2 days ago start-of-day until now
        val now = Instant.now()
        val start = LocalDate.now().minusDays(2).atStartOfDay().toInstant(zone)

        // read 1-minute buckets for all selected metrics
        val buckets: List<AggregationResultGroupedByDuration> = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = metricsSet,
                timeRangeFilter = TimeRangeFilter.between(start, now),
                timeRangeSlicer = Duration.ofMinutes(1)
            )
        )

        // group buckets by date string "YYYY-MM-DD"
        buckets
            .sortedBy { it.startTime }
            .groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
            .forEach { (_, dayBuckets) ->
                // build cumulative entries ( total and active calories)
                val cumEntries = dayBuckets.map { bucket ->
                    CumulativeEntry(
                        start = bucket.startTime.toString(),
                        totalCalories = bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                            ?.inKilocalories
                            ?.roundTo(2),
                        activeCalories = bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                            ?.inKilocalories
                            ?.roundTo(2)
                    )
                }.filter { it.totalCalories != null || it.activeCalories != null }

                // build statistical entries (e.g. heart rate)
                val statEntries = dayBuckets.mapNotNull { bucket ->
                    val min = bucket.result[HeartRateRecord.BPM_MIN]?.toDouble()
                    val avg = bucket.result[HeartRateRecord.BPM_AVG]?.toDouble()
                    val max = bucket.result[HeartRateRecord.BPM_MAX]?.toDouble()
                    if (min != null || avg != null || max != null) {
                        StatisticalEntry(
                            start = bucket.startTime.toString(),
                            min = min ?: 0.0,
                            avg = avg ?: 0.0,
                            max = max ?: 0.0
                        )
                    } else null
                }
                // collect distinct sources
                val origins = dayBuckets
                    .flatMap { it.result.dataOrigins.map { od -> od.packageName } }
                    .distinct()

                // assemble payload
                val payload = HealthConnectIntradayPayload(
                    cumulative = cumEntries.takeIf { it.isNotEmpty() }
                        ?.let { IntradayDataBase(origins, it) },
                    statistical = statEntries.takeIf { it.isNotEmpty() }
                        ?.let { IntradayDataBase(origins, it) }
                )

                // upload and throw on error
                val result = service.uploadHealthConnectIntraday(payload).execute()
                if (result.isError) throw result.error!!
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
                StatisticalEntry(
                    start = null,
                    min = minHr?.toDouble(),
                    avg = avgHr?.toDouble(),
                    max = maxHr?.toDouble()
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

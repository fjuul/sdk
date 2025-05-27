package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
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
import java.time.Duration
import java.time.Period
import java.time.ZoneOffset

class HealthConnectDataManager(
    private val client: HealthConnectClient,
    private val service: ActivitySourcesService
) {
    /** Synchronize hourly intraday aggregates, split per day. */
    suspend fun syncIntraday(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val metricsSet = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()
        val zone = ZoneOffset.UTC

        // fetch all hourly buckets in the full range
        val allBuckets: List<AggregationResultGroupedByDuration> =
            client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metricsSet,
                    timeRangeFilter = TimeRangeFilter.between(
                        options.timeRangeStart.atStartOfDay().toInstant(zone),
                        options.timeRangeEnd.plusDays(1).atStartOfDay().toInstant(zone)
                    ),
                    timeRangeSlicer = Duration.ofHours(1)
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
            bucket.result[HeartRateRecord.BPM_MIN]?.let { map["heartRateMin"] = it.toDouble() }
            bucket.result[HeartRateRecord.BPM_AVG]?.let { map["heartRateAvg"] = it.toDouble() }
            bucket.result[HeartRateRecord.BPM_MAX]?.let { map["heartRateMax"] = it.toDouble() }

            IntradayEntry(
                start = bucket.startTime.toString(), // ISO 8601
                dataOrigins = bucket.result.dataOrigins.map { it.packageName },
                metrics = map
            )
        }

        // group by local date and upload one batch per day
        allEntries
            .groupBy { it.start.substring(0, 10) /* "YYYY-MM-DD" */ }
            .forEach { (_, entriesForDate) ->
                val result = service
                    .uploadHealthConnectIntraday(HealthConnectIntradayPayload(entriesForDate))
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

        // use LocalDateTime for period-based aggregation
        val startDateTime = options.timeRangeStart.atStartOfDay()
        val endDateTime = options.timeRangeEnd.plusDays(1).atStartOfDay()

        val raw: List<AggregationResultGroupedByPeriod> =
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = metricsSet,
                    timeRangeFilter = TimeRangeFilter.between(startDateTime, endDateTime),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        val entries = raw.map { bucket ->
            val minHr = bucket.result[HeartRateRecord.BPM_MIN]
            val avgHr = bucket.result[HeartRateRecord.BPM_AVG]
            val maxHr = bucket.result[HeartRateRecord.BPM_MAX]
            val stat = if (minHr != null || avgHr != null || maxHr != null) {
                StatisticalAggregateValue(
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

        val result = service
            .uploadHealthConnectDailies(HealthConnectDailiesPayload(entries))
            .execute()

        if (result.isError) {
            throw result.error!!
        }
    }

    /** Sync only the latest height (in cm) & weight (in kg). */
    suspend fun syncProfile(options: HealthConnectSyncOptions) {
        val zone = ZoneOffset.UTC
        val start = options.timeRangeStart.atStartOfDay().toInstant(zone)
        val end = options.timeRangeEnd.plusDays(1).atStartOfDay().toInstant(zone)

        // read all height records
        val heightsResp: ReadRecordsResponse<HeightRecord> = client.readRecords(
            ReadRecordsRequest(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        // read all weight records
        val weightsResp: ReadRecordsResponse<WeightRecord> = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )

        // pick most recent height in meters → convert to cm
        val latestHeightCm: Double? = heightsResp.records
            .maxByOrNull { it.time }
            ?.height
            ?.inMeters
            ?.times(100.0)

        // pick most recent weight in kg
        val latestWeightKg: Double? = weightsResp.records
            .maxByOrNull { it.time }
            ?.weight
            ?.inKilograms

        if (latestHeightCm == null && latestWeightKg == null) return

        service.uploadHealthConnectProfile(
            HealthConnectProfilePayload(
                height = latestHeightCm,
                weight = latestWeightKg
            )
        ).execute()
    }
}

/** Map our enum to Health Connect aggregate metrics. */
fun FitnessMetricsType.toAggregateMetrics(): Set<AggregateMetric<*>> = when (this) {
    FitnessMetricsType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)
    FitnessMetricsType.INTRADAY_CALORIES -> setOf(
        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
    )

    FitnessMetricsType.INTRADAY_HEART_RATE -> setOf(
        HeartRateRecord.BPM_MIN,
        HeartRateRecord.BPM_AVG,
        HeartRateRecord.BPM_MAX
    )

    else -> throw HealthConnectException.UnsupportedMetricException(this.name)
}

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
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import java.time.Duration
import java.time.Period
import java.time.ZoneOffset

/**
 * Reads aggregated data from Health Connect and uploads it via ActivitySourcesService.
 */
class HealthConnectDataManager(
    private val client: HealthConnectClient,
    private val service: ActivitySourcesService
) {

    /**
     * Synchronizes intraday metrics in hourly buckets.
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) {
            throw HealthConnectException.NoMetricsSelectedException()
        }
        val metrics = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()
        val start = options.timeRangeStart.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = options.timeRangeEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        val intradayResponse: List<AggregationResultGroupedByDuration> =
            client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofHours(1)
                )
            )

        service.uploadHealthConnectIntraday(
            HealthConnectIntradayData(intradayStats = intradayResponse)
        )
    }

    /**
     * Synchronizes daily metrics in one-day buckets.
     */
    suspend fun syncDaily(options: HealthConnectSyncOptions) {
        if (options.metrics.isEmpty()) {
            throw HealthConnectException.NoMetricsSelectedException()
        }
        val metrics = options.metrics.flatMap { it.toAggregateMetrics() }.toSet()
        val start = options.timeRangeStart.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = options.timeRangeEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        val dailyResponse: List<AggregationResultGroupedByPeriod> =
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        service.uploadHealthConnectDailies(
            HealthConnectDailiesData(dailyStats = dailyResponse)
        )
    }

    /**
     * Synchronizes profile data (height & weight).
     */
    suspend fun syncProfile(options: HealthConnectSyncOptions) {
        val start = options.timeRangeStart.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = options.timeRangeEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        // Read height records
        val heightsResponse: ReadRecordsResponse<HeightRecord> =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
        val heights: List<HeightRecord> = heightsResponse.records

        // Read weight records
        val weightsResponse: ReadRecordsResponse<WeightRecord> =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
        val weights: List<WeightRecord> = weightsResponse.records

        service.uploadHealthConnectProfile(
            HealthConnectProfileData(
                heights = heights,
                weights = weights
            )
        )
    }
}


/**
 * Maps our enum values to Health Connect aggregate metrics.
 * Throws UnsupportedMetricException for any metric that cannot be aggregated.
 */
fun FitnessMetricsType.toAggregateMetrics(): Set<AggregateMetric<*>> = when (this) {
    // Daily steps aggregation
    FitnessMetricsType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)

    // Intraday calories: both total and active
    FitnessMetricsType.INTRADAY_CALORIES -> setOf(
        TotalCaloriesBurnedRecord.ENERGY_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
    )

    // Intraday heart rate statistics (average)
    FitnessMetricsType.INTRADAY_HEART_RATE -> setOf(HeartRateRecord.BPM_AVG)

    // The following metrics are not supported for aggregate queries:
    FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.WORKOUTS, FitnessMetricsType.HEIGHT, FitnessMetricsType.WEIGHT -> throw HealthConnectException.UnsupportedMetricException(
        this.name
    )
}

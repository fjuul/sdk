package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Converts raw HealthConnectDataPoint values into uploadable payloads.
 * Handles aggregation logic and DTO construction.
 */
object HealthConnectDataMapper {

    fun toCumulativePayload(
        points: List<HealthConnectDataPoint>,
        type: FitnessMetricsType,
        dataOrigins: List<String>
    ): HealthConnectIntradayPayload<IntradayCumulativeEntry>? {
        val entries = points.filter { it.type == type }
            .groupBy { it.startTime.truncatedTo(ChronoUnit.MINUTES) }
            .map { (time, group) ->
                IntradayCumulativeEntry(
                    start = Date.from(time),
                    value = group.sumOf { it.value }
                )
            }

        return entries.takeIf { it.isNotEmpty() }?.let {
            HealthConnectIntradayPayload(dataOrigins, it)
        }
    }

    fun toStatisticalPayload(
        points: List<HealthConnectDataPoint>,
        type: FitnessMetricsType,
        dataOrigins: List<String>
    ): HealthConnectIntradayPayload<IntradayStatisticalEntry>? {
        val entries = points.filter { it.type == type }
            .groupBy { it.startTime.truncatedTo(ChronoUnit.MINUTES) }
            .mapNotNull { (time, group) ->
                val values = group.map { it.value }
                if (values.isEmpty()) null else IntradayStatisticalEntry(
                    start = Date.from(time),
                    min = values.minOrNull() ?: return@mapNotNull null,
                    avg = values.average(),
                    max = values.maxOrNull() ?: return@mapNotNull null
                )
            }

        return entries.takeIf { it.isNotEmpty() }?.let {
            HealthConnectIntradayPayload(dataOrigins, it)
        }
    }

    fun toDailyPayload(
        points: List<HealthConnectDataPoint>,
        date: String,
        dataOrigins: List<String>
    ): HealthConnectDailiesPayload {
        val steps = points.filter { it.type == FitnessMetricsType.STEPS }
            .sumOf { it.value.toInt() }
            .takeIf { it > 0 }

        val hr = points.filter { it.type == FitnessMetricsType.INTRADAY_HEART_RATE }.map { it.value }

        val restingHR = if (hr.isNotEmpty()) {
            StatisticalValue(
                min = hr.minOrNull() ?: 0.0,
                avg = hr.average(),
                max = hr.maxOrNull() ?: 0.0
            )
        } else null

        return HealthConnectDailiesPayload(
            date = date,
            dataOrigins = dataOrigins,
            steps = steps,
            restingHeartRate = restingHR
        )
    }

    fun toProfilePayload(points: List<HealthConnectDataPoint>): HealthConnectProfilePayload? {
        val latestHeight = points.filter { it.type == FitnessMetricsType.HEIGHT }
            .maxByOrNull { it.startTime }?.value

        val latestWeight = points.filter { it.type == FitnessMetricsType.WEIGHT }
            .maxByOrNull { it.startTime }?.value

        if (latestHeight == null && latestWeight == null) return null

        return HealthConnectProfilePayload(height = latestHeight, weight = latestWeight)
    }
}

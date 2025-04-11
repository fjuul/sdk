package com.fjuul.sdk.activitysources.healthconnect.model

import java.time.LocalDate

/**
 * Represents daily aggregated data points.
 */
public data class DailyAggregateData(
    override val date: LocalDate,
    override val dataOrigins: Set<String>,
    override val steps: Long? = null,
    override val restingHeartRate: HealthConnectStatisticalAggregateValue? = null
) : HealthConnectDailyData {
    public companion object {
        /**
         * Creates a DailyAggregateData instance with steps data.
         */
        public fun forSteps(
            date: LocalDate,
            dataOrigins: Set<String>,
            steps: Long
        ): DailyAggregateData = DailyAggregateData(
            date = date,
            dataOrigins = dataOrigins,
            steps = steps
        )

        /**
         * Creates a DailyAggregateData instance with resting heart rate data.
         */
        public fun forRestingHeartRate(
            date: LocalDate,
            dataOrigins: Set<String>,
            min: Double,
            avg: Double,
            max: Double
        ): DailyAggregateData = DailyAggregateData(
            date = date,
            dataOrigins = dataOrigins,
            restingHeartRate = object : HealthConnectStatisticalAggregateValue {
                override val min: Double = min
                override val avg: Double = avg
                override val max: Double = max
            }
        )
    }
} 
package com.fjuul.sdk.activitysources.healthconnect.model

import java.time.LocalDate

/**
 * Interface for daily aggregated data points.
 */
public interface HealthConnectDailyData {
    public val date: LocalDate
    public val dataOrigins: Set<String>
    public val steps: Long?
    public val restingHeartRate: HealthConnectStatisticalAggregateValue?
} 
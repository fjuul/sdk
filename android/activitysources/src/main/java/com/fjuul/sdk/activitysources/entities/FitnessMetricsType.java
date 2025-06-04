package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * General enumeration of activity metrics and human's measurements used in Fjuul SDK.
 */
public enum FitnessMetricsType {
    INTRADAY_CALORIES,
    INTRADAY_HEART_RATE,
    INTRADAY_STEPS,
    /**
     * Represents the total number of steps taken during a specific time period. This metric is specifically used for
     * Health Connect API integration to track step count data. Unlike INTRADAY_STEPS which is used for intraday step
     * tracking, this metric is optimized for Health Connect's data structure and provides aggregated step count
     * information.
     */
    STEPS,
    /**
     * Represents the user's resting heart rate aggregated daily from Health Connect API. This metric provides a
     * statistical summary (min, avg, max BPM) for each day and is used to sync resting heart rate as a daily data
     * point.
     */
    RESTING_HEART_RATE,
    WORKOUTS,

    HEIGHT,
    WEIGHT;

    public static boolean isIntradayMetricType(@NonNull FitnessMetricsType metricType) {
        switch (metricType) {
            case INTRADAY_HEART_RATE:
            case INTRADAY_CALORIES:
            case INTRADAY_STEPS:
                return true;
            default:
                return false;
        }
    }

    public static boolean isProfileMetricType(@NonNull FitnessMetricsType metricsType) {
        switch (metricsType) {
            case HEIGHT:
            case WEIGHT:
                return true;
            default:
                return false;
        }
    }
}

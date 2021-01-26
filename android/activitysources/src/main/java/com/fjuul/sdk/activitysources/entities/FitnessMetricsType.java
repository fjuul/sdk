package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * General enumeration of activity metrics used in Fjuul SDK.
 */
public enum FitnessMetricsType {
    INTRADAY_CALORIES,
    INTRADAY_HEART_RATE,
    INTRADAY_STEPS,
    WORKOUTS;

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
}

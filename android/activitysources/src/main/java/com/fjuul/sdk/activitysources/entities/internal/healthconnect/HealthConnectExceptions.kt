package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.core.exceptions.FjuulException

/**
 * Base exception for all Health Connect related errors.
 */
sealed class HealthConnectException(message: String) : FjuulException(message) {
    /**
     * Thrown when Health Connect is not supported on the device.
     */
    class NotSupportedException : HealthConnectException(
        "Health Connect is not supported on this device"
    )

    /**
     * Thrown when Health Connect is supported but not installed.
     */
    class NotInstalledException : HealthConnectException(
        "Health Connect is not installed. Please install it from Play Store"
    )

    /**
     * Thrown when required permissions are not granted.
     */
    class PermissionsNotGrantedException : HealthConnectException(
        "Required permissions not granted"
    )

    /**
     * Thrown when no metrics are selected for sync.
     */
    class NoMetricsSelectedException: HealthConnectException(
        "No metrics selected"
    )

    /** Thrown when a requested metric is not supported by Health Connect. */
    class UnsupportedMetricException(metricName: String) : HealthConnectException(
        "Metric '$metricName' is not supported by Health Connect"
    )

    /**
     * Thrown when Health Connect is not supported on this device.
     */
    class UnsupportedHealthConnectException: HealthConnectException(
        "Health Connect not supported"
    )
}

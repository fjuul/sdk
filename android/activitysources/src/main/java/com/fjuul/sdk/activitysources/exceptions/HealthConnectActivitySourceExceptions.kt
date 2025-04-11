package com.fjuul.sdk.activitysources.exceptions

import com.fjuul.sdk.core.exceptions.FjuulException

/**
 * Collection of exceptions specific to Health Connect activity source.
 */
object HealthConnectActivitySourceExceptions {
    /**
     * Exception thrown when not all required Health Connect permissions are granted.
     */
    class HealthConnectPermissionsNotGrantedException(message: String) : FjuulException(message)

    /**
     * Exception thrown when a common error occurs during Health Connect operations.
     */
    class CommonException(message: String) : FjuulException(message)
} 
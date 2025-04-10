package com.fjuul.sdk.activitysources.exceptions;

import com.fjuul.sdk.core.exceptions.FjuulException;
import androidx.annotation.NonNull;

/**
 * A class that contains all exceptions related to the Health Connect activity source.
 */
public class HealthConnectActivitySourceExceptions {
    /**
     * An exception that indicates that not all required Health Connect permissions were granted by the user.
     */
    public static class HealthConnectPermissionsNotGrantedException extends FjuulException {
        public HealthConnectPermissionsNotGrantedException(@NonNull String message) {
            super(message);
        }
    }

    /**
     * A common exception that can be thrown during the work with Health Connect.
     */
    public static class CommonException extends FjuulException {
        public CommonException(@NonNull String message) {
            super(message);
        }
    }
} 
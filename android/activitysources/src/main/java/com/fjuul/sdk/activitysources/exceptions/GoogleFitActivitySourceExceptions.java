package com.fjuul.sdk.activitysources.exceptions;

import androidx.annotation.NonNull;

import com.fjuul.sdk.exceptions.FjuulException;

public final class GoogleFitActivitySourceExceptions {
    public static class CommonException extends FjuulException {
        public CommonException(@NonNull String message) {
            super(message);
        }

        public CommonException(@NonNull Throwable throwable) {
            super(throwable);
        }
    }

    public static class MaxRetriesExceededException extends CommonException {
        public MaxRetriesExceededException(@NonNull String message) {
            super(message);
        }
    }

    public static class FitnessPermissionsNotGrantedException extends CommonException {
        public FitnessPermissionsNotGrantedException(@NonNull String message) {
            super(message);
        }
    }

    public static class ActivityRecognitionPermissionNotGrantedException extends CommonException {
        public ActivityRecognitionPermissionNotGrantedException(@NonNull String message) {
            super(message);
        }
    }
}
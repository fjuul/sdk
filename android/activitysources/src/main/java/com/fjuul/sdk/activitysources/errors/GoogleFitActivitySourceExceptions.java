package com.fjuul.sdk.activitysources.errors;

import androidx.annotation.NonNull;

import com.fjuul.sdk.errors.FjuulError;

public final class GoogleFitActivitySourceExceptions {
    public static class CommonException extends FjuulError {
        public CommonException(@NonNull String message) {
            super(message);
        }
    }

    public static class MaxRetriesExceededException extends CommonException {
        public MaxRetriesExceededException(@NonNull String message) {
            super(message);
        }
    }

    public static class NotGrantedPermissionsException extends CommonException {
        public NotGrantedPermissionsException(@NonNull String message) {
            super(message);
        }
    }
}

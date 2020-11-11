package com.fjuul.sdk.activitysources.exceptions;

import androidx.annotation.NonNull;

import com.fjuul.sdk.exceptions.FjuulException;

public final class GoogleFitActivitySourceExceptions {
    public static class CommonException extends FjuulException {
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

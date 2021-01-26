package com.fjuul.sdk.activitysources.exceptions;

import com.fjuul.sdk.core.exceptions.ApiExceptions;

import androidx.annotation.NonNull;

public final class ActivitySourcesApiExceptions {
    public static class SourceAlreadyConnectedException extends ApiExceptions.CommonException {
        public SourceAlreadyConnectedException(@NonNull String message) {
            super(message);
        }
    }
}

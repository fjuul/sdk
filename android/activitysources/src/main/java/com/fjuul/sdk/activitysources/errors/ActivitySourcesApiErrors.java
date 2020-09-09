package com.fjuul.sdk.activitysources.errors;

import com.fjuul.sdk.errors.ApiErrors;

import androidx.annotation.NonNull;

public final class ActivitySourcesApiErrors {
    public static class SourceAlreadyConnectedError extends ApiErrors.CommonError {
        public SourceAlreadyConnectedError(@NonNull String message) {
            super(message);
        }
    }
}

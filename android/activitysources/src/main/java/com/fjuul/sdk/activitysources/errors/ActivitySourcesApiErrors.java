package com.fjuul.sdk.activitysources.errors;

import androidx.annotation.NonNull;

import com.fjuul.sdk.errors.ApiErrors;

public final class ActivitySourcesApiErrors {
    public static class SourceAlreadyConnectedError extends ApiErrors.CommonError {
        public SourceAlreadyConnectedError(@NonNull String message) {
            super(message);
        }
    }
}

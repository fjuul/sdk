package com.fjuul.sdk.user.exceptions;

import androidx.annotation.NonNull;

import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.user.http.responses.ValidationError;

import java.util.List;

public final class UserApiExceptions {
    public static class ValidationErrorBadRequestException extends ApiExceptions.BadRequestException {
        private @NonNull List<ValidationError> errors;

        public ValidationErrorBadRequestException(@NonNull String message, @NonNull List<ValidationError> errors) {
            super(message);
            this.errors = errors;
        }

        @NonNull
        public List<ValidationError> getErrors() {
            return errors;
        }
    }
}

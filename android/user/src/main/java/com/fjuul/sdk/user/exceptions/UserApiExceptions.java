package com.fjuul.sdk.user.exceptions;

import java.util.List;

import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.user.http.responses.ValidationError;

import androidx.annotation.NonNull;

public final class UserApiExceptions {
    /**
     * Exception used in the case of providing invalid input parameters of the user profile.
     */
    public static class ValidationErrorBadRequestException extends ApiExceptions.BadRequestException {
        private @NonNull List<ValidationError> errors;

        public ValidationErrorBadRequestException(@NonNull String message, @NonNull List<ValidationError> errors) {
            super(message);
            this.errors = errors;
        }

        /**
         * Returns a list of all validation errors for the submitted user profile data.
         *
         * @return validation errors
         */
        @NonNull
        public List<ValidationError> getErrors() {
            return errors;
        }
    }
}

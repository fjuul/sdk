package com.fjuul.sdk.user.http.responses;

import androidx.annotation.NonNull;

import java.util.List;

import com.fjuul.sdk.core.http.responses.ErrorJSONBodyResponse;

public class ValidationErrorJSONBodyResponse extends ErrorJSONBodyResponse {
    @NonNull private List<ValidationError> errors;

    @NonNull
    public List<ValidationError> getErrors() {
        return errors;
    }
}

package com.fjuul.sdk.user.http.responses;

import androidx.annotation.NonNull;

import java.util.Map;

public class ValidationError {
    private @NonNull String property;
    private @NonNull Object value;
    private @NonNull Map<String, String> constraints;

    @NonNull
    public String getProperty() {
        return property;
    }

    @NonNull
    public Object getValue() {
        return value;
    }

    @NonNull
    public Map<String, String> getConstraints() {
        return constraints;
    }
}

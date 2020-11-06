package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public abstract class ActivitySource {
    @NonNull
    protected abstract String getRawValue();
}

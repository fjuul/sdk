package com.fjuul.sdk.core.adapters;

import java.io.IOException;
import java.util.TimeZone;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import androidx.annotation.NonNull;

public class TimeZoneJsonAdapter {
    @FromJson
    @NonNull
    public TimeZone fromJson(@NonNull String timezone) throws IOException {
        return TimeZone.getTimeZone(timezone);
    }

    @ToJson
    @NonNull
    public String toJson(@NonNull TimeZone value) throws IOException {
        return value.getID();
    }
}

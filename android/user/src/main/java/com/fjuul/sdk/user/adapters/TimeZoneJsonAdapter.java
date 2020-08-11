package com.fjuul.sdk.user.adapters;

import androidx.annotation.Nullable;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.io.IOException;
import java.util.TimeZone;

public class TimeZoneJsonAdapter {
    @Nullable
    @FromJson
    public TimeZone fromJson(String timezone) throws IOException {
        return TimeZone.getTimeZone(timezone);
    }

    @ToJson
    public String toJson(@Nullable TimeZone value) throws IOException {
        return value.getID();
    }
}

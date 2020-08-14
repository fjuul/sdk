package com.fjuul.sdk.user.adapters;

import java.io.IOException;
import java.time.LocalDate;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;

public class LocalDateJsonAdapter {
    @SuppressLint("NewApi")
    @Nullable
    @FromJson
    public LocalDate fromJson(String dateString) throws IOException {
        return LocalDate.parse(dateString);
    }

    @ToJson
    public String toJson(@Nullable LocalDate value) throws IOException {
        return value.toString();
    }
}

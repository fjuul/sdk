package com.fjuul.sdk.user.adapters;

import java.io.IOException;
import java.time.LocalDate;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class LocalDateJsonAdapter {
    @SuppressLint("NewApi")
    @FromJson
    @NonNull
    public LocalDate fromJson(@NonNull String dateString) throws IOException {
        return LocalDate.parse(dateString);
    }

    @ToJson
    @NonNull
    public String toJson(@NonNull LocalDate value) throws IOException {
        return value.toString();
    }
}

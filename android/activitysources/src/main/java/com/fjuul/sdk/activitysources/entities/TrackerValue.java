package com.fjuul.sdk.activitysources.entities;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TrackerValue carries the raw string presentation of the tracker and contains predefined static constants for known
 * activity sources in terms of the current SDK version and platform.
 */
@SuppressLint("NewApi")
public class TrackerValue {
    public static final TrackerValue FITBIT = new TrackerValue("fitbit");
    public static final TrackerValue GARMIN = new TrackerValue("garmin");
    public static final TrackerValue OURA = new TrackerValue("oura");
    public static final TrackerValue POLAR = new TrackerValue("polar");
    public static final TrackerValue GOOGLE_FIT = new TrackerValue("googlefit");
    public static final TrackerValue SUUNTO = new TrackerValue("suunto");
    public static final TrackerValue WITHINGS = new TrackerValue("withings");
    public static final TrackerValue HEALTH_CONNECT = new TrackerValue("healthconnect");

    static final List<TrackerValue> constants;

    static {
        constants = Arrays.stream(TrackerValue.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType().equals(TrackerValue.class))
            .map(field -> {
                try {
                    return (TrackerValue) field.get(null);
                } catch (IllegalAccessException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }

    @NonNull
    private final String value;

    public TrackerValue(@NonNull String value) {
        this.value = value;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @SuppressLint("NewApi")
    @Nullable
    public static TrackerValue forValue(@NonNull String value) {
        return constants.stream().filter(t -> t.value.equals(value)).findFirst().orElse(null);
    }

    @NonNull
    public static List<TrackerValue> values() {
        return Collections.unmodifiableList(constants);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TrackerValue that = (TrackerValue) o;

        return value.equals(that.value);
    }

    @Override
    public String toString() {
        return "TrackerValue{value='" + value + "\'}";
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

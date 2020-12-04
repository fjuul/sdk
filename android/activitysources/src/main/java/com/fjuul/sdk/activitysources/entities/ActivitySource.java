package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.stream.Stream;

public abstract class ActivitySource {
    public enum TrackerValue {
        FITBIT("fitbit"),
        GARMIN("garmin"),
        POLAR("polar"),
        GOOGLE_FIT("googlefit");

        @NonNull
        private final String value;
        TrackerValue(@NonNull String value) {
            this.value = value;
        }

        @NonNull
        public String getValue() {
            return value;
        }

        @SuppressLint("NewApi")
        @Nullable
        public static TrackerValue forValue(@NonNull String value) {
            return Stream.of(TrackerValue.values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElse(null);
        }
    }

    @NonNull
    protected abstract TrackerValue getTrackerValue();
}

package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.stream.Stream;

/**
 * A base class for all activity source classes.
 * @see FitbitActivitySource
 * @see GarminActivitySource
 * @see GoogleFitActivitySource
 * @see PolarActivitySource
 * @see SuuntoActivitySource
 */
public abstract class ActivitySource {
    /**
     * Enum which maps an activity source to the underlying string presentation (tracker value).
     */
    public enum TrackerValue {
        FITBIT("fitbit"),
        GARMIN("garmin"),
        POLAR("polar"),
        GOOGLE_FIT("googlefit"),
        SUUNTO("suunto");

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

    /**
     * Returns the tracker value of the ActivitySource class.
     * @return
     */
    @NonNull
    protected abstract TrackerValue getTrackerValue();
}

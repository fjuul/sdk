package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Garmin tracker. This is an external activity source.
 */
public class GarminActivitySource extends ActivitySource {
    private GarminActivitySource() {}

    @NonNull
    public static GarminActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.GARMIN;
    }

    private static class SingletonHolder {
        private static final GarminActivitySource instance = new GarminActivitySource();
    }
}

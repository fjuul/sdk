package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Strava tracker. This is an external activity source.
 */
public class StravaActivitySource extends ActivitySource {
    private StravaActivitySource() {}

    @NonNull
    public static StravaActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.STRAVA;
    }

    private static class SingletonHolder {
        private static final StravaActivitySource instance = new StravaActivitySource();
    }
}

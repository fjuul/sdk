package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Google Health tracker. This is an external activity source.
 */
public class GoogleHealthActivitySource extends ActivitySource {
    private GoogleHealthActivitySource() {}

    @NonNull
    public static GoogleHealthActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.GOOGLE_HEALTH;
    }

    private static class SingletonHolder {
        private static final GoogleHealthActivitySource instance = new GoogleHealthActivitySource();
    }
}

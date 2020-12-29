package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Fitbit tracker. This is an external activity source.
 */
public class FitbitActivitySource extends ActivitySource {
    private FitbitActivitySource() {}

    @NonNull
    public static FitbitActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.FITBIT;
    }

    private static class SingletonHolder {
        private static final FitbitActivitySource instance = new FitbitActivitySource();
    }
}

package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

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

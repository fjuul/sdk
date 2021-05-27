package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Withings tracker. This is an external activity source.
 */
public class WithingsActivitySource extends ActivitySource {
    private WithingsActivitySource() {}

    @NonNull
    public static WithingsActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.WITHINGS;
    }

    private static class SingletonHolder {
        private static final WithingsActivitySource instance = new WithingsActivitySource();
    }
}

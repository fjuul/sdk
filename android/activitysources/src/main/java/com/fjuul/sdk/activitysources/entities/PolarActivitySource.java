package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Polar tracker. This is an external activity source.
 */
public class PolarActivitySource extends ActivitySource {
    private PolarActivitySource() {}

    @NonNull
    public static PolarActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.POLAR;
    }

    private static class SingletonHolder {
        private static final PolarActivitySource instance = new PolarActivitySource();
    }
}


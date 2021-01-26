package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Suunto tracker. This is an external activity source.
 */
public class SuuntoActivitySource extends ActivitySource {
    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.SUUNTO;
    }

    private SuuntoActivitySource() {}

    @NonNull
    public static SuuntoActivitySource getInstance() {
        return SuuntoActivitySource.SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final SuuntoActivitySource instance = new SuuntoActivitySource();
    }
}

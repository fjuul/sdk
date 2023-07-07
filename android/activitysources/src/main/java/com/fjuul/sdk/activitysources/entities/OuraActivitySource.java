package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class for the Oura tracker. This is an external activity source.
 */
public class OuraActivitySource extends ActivitySource {
    private OuraActivitySource() {}

    @NonNull
    public static OuraActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return TrackerValue.OURA;
    }

    private static class SingletonHolder {
        private static final OuraActivitySource instance = new OuraActivitySource();
    }
}

package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public class GarminActivitySource extends ActivitySource {
    private GarminActivitySource() {}

    @NonNull
    public static GarminActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected String getRawValue() {
        return "garmin";
    }

    private static class SingletonHolder {
        private static final GarminActivitySource instance = new GarminActivitySource();
    }
}

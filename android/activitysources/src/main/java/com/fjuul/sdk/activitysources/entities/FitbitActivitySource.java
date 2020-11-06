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
    protected String getRawValue() {
        return "fitbit";
    }

    private static class SingletonHolder {
        private static final FitbitActivitySource instance = new FitbitActivitySource();
    }
}

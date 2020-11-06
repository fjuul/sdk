package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public class PolarActivitySource extends ActivitySource {
    private PolarActivitySource() {}

    @NonNull
    public static PolarActivitySource getInstance() {
        return SingletonHolder.instance;
    }

    @NonNull
    @Override
    protected String getRawValue() {
        return "polar";
    }

    private static class SingletonHolder {
        private static final PolarActivitySource instance = new PolarActivitySource();
    }
}


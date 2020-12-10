package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class ActivitySourceResolver {
    @Nullable
    public ActivitySource getInstanceByTrackerValue(@NonNull String trackerValue) {
        switch (ActivitySource.TrackerValue.forValue(trackerValue)) {
            case POLAR: return PolarActivitySource.getInstance();
            case FITBIT: return FitbitActivitySource.getInstance();
            case GARMIN: return GarminActivitySource.getInstance();
            case GOOGLE_FIT: return GoogleFitActivitySource.getInstance();
            default: return null;
        }
    }
}

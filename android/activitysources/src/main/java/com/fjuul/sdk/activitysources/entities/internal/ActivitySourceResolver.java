package com.fjuul.sdk.activitysources.entities.internal;

import com.fjuul.sdk.activitysources.entities.ActivitySource;
import com.fjuul.sdk.activitysources.entities.FitbitActivitySource;
import com.fjuul.sdk.activitysources.entities.GarminActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.PolarActivitySource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ActivitySourceResolver {
    @Nullable
    public ActivitySource getInstanceByTrackerValue(@NonNull String trackerValue) {
        switch (ActivitySource.TrackerValue.forValue(trackerValue)) {
            case POLAR:
                return PolarActivitySource.getInstance();
            case FITBIT:
                return FitbitActivitySource.getInstance();
            case GARMIN:
                return GarminActivitySource.getInstance();
            case GOOGLE_FIT:
                return GoogleFitActivitySource.getInstance();
            default:
                return null;
        }
    }
}

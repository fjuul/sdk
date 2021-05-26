package com.fjuul.sdk.activitysources.entities.internal;

import com.fjuul.sdk.activitysources.entities.ActivitySource;
import com.fjuul.sdk.activitysources.entities.FitbitActivitySource;
import com.fjuul.sdk.activitysources.entities.GarminActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.PolarActivitySource;
import com.fjuul.sdk.activitysources.entities.SuuntoActivitySource;
import com.fjuul.sdk.activitysources.entities.WithingsActivitySource;
import com.fjuul.sdk.activitysources.entities.TrackerValue;
import com.fjuul.sdk.activitysources.entities.UnknownActivitySource;

import androidx.annotation.NonNull;

public class ActivitySourceResolver {
    @NonNull
    public ActivitySource getInstanceByTrackerValue(@NonNull String trackerValue) {
        final TrackerValue tracker = TrackerValue.forValue(trackerValue);
        if (TrackerValue.POLAR.equals(tracker)) {
            return PolarActivitySource.getInstance();
        } else if (TrackerValue.FITBIT.equals(tracker)) {
            return FitbitActivitySource.getInstance();
        } else if (TrackerValue.GARMIN.equals(tracker)) {
            return GarminActivitySource.getInstance();
        } else if (TrackerValue.SUUNTO.equals(tracker)) {
            return SuuntoActivitySource.getInstance();
        } else if (TrackerValue.WITHINGS.equals(tracker)) {
            return WithingsActivitySource.getInstance();
        } else if (TrackerValue.GOOGLE_FIT.equals(tracker)) {
            return GoogleFitActivitySource.getInstance();
        }
        return new UnknownActivitySource(new TrackerValue(trackerValue));
    }
}

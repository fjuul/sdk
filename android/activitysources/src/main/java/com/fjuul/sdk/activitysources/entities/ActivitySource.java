package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * A base class for all activity source classes.
 *
 * @see FitbitActivitySource
 * @see GarminActivitySource
 * @see GoogleFitActivitySource
 * @see HealthConnectActivitySource
 * @see OuraActivitySource
 * @see PolarActivitySource
 * @see StravaActivitySource
 * @see SuuntoActivitySource
 * @see WithingsActivitySource
 */
public abstract class ActivitySource {
    /**
     * Returns the tracker value of the ActivitySource class.
     *
     * @return tracker value
     */
    @NonNull
    protected abstract TrackerValue getTrackerValue();
}

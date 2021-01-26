package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * A base class for all activity source classes.
 *
 * @see FitbitActivitySource
 * @see GarminActivitySource
 * @see GoogleFitActivitySource
 * @see PolarActivitySource
 * @see SuuntoActivitySource
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

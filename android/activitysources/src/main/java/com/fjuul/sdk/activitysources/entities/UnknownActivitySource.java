package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * The ActivitySource class with unsupported tracker value. There may be a case when the current SDK version does not
 * recognize the received tracker connection and cannot provide the appropriate ActivitySource instance for that. In
 * this case, {@code UnknownActivitySource} will be matched for that connection.
 */
public class UnknownActivitySource extends ActivitySource {
    private TrackerValue trackerValue;

    @NonNull
    @Override
    protected TrackerValue getTrackerValue() {
        return trackerValue;
    }

    public UnknownActivitySource(@NonNull TrackerValue trackerValue) {
        this.trackerValue = trackerValue;
    }
}

package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public class ActivitySourceConnection extends TrackerConnection {
    @NonNull
    private final ActivitySource activitySource;

    public ActivitySourceConnection(@NonNull TrackerConnection trackerConnection, @NonNull ActivitySource activitySource) {
        super(trackerConnection.getId(), trackerConnection.getTracker(), trackerConnection.getCreatedAt(), trackerConnection.getEndedAt());
        this.activitySource = activitySource;
    }

    @NonNull
    public ActivitySource getActivitySource() {
        return activitySource;
    }
}

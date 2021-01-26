package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

/**
 * A tracker connection bound with ActivitySource. To determine whether an instance is related to the particular
 * ActivitySource class (e.g. GoogleFitActivitySource, GarminActivitySource, etc), you can:
 * <ul>
 * <li>check an instance of #getActivitySource with java's {@code instanceof} operator;
 *
 * <pre>
 * {@code if (getActivitySource() instanceof GoogleFitActivitySource) { do something }}
 * </pre>
 *
 * </li>
 * <li>compare a value of {@link #getTracker} with the desired static constant of {@link TrackerValue}.
 *
 * <pre>
 * {@code
 * ActivitySourceConnection sourceConnection;
 * TrackerValue.GOOGLE_FIT.getValue().equals(sourceConnection.getTracker())
 * }
 * </pre>
 *
 * </li>
 * </ul>
 */
public class ActivitySourceConnection extends TrackerConnection {
    @NonNull
    private final ActivitySource activitySource;

    public ActivitySourceConnection(@NonNull TrackerConnection trackerConnection,
        @NonNull ActivitySource activitySource) {
        super(trackerConnection.getId(),
            trackerConnection.getTracker(),
            trackerConnection.getCreatedAt(),
            trackerConnection.getEndedAt());
        this.activitySource = activitySource;
    }

    /**
     * NOTE: Keep in mind that for the unsupported tracker connection by the SDK, the method returns an instance of
     * {@link UnknownActivitySource}.
     * @return instance of the ActivitySource
     */
    @NonNull
    public ActivitySource getActivitySource() {
        return activitySource;
    }
}

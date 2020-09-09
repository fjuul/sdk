package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

public abstract class ConnectionResult {
    private ConnectionResult() {}

    public static class ExternalAuthenticationFlowRequired extends ConnectionResult {
        @NonNull
        private String url;

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    public static class Connected extends ConnectionResult {
        @NonNull
        private TrackerConnection trackerConnection;

        public Connected(@NonNull TrackerConnection trackerConnection) {
            this.trackerConnection = trackerConnection;
        }

        @NonNull
        public TrackerConnection getTrackerConnection() {
            return trackerConnection;
        }
    }
}

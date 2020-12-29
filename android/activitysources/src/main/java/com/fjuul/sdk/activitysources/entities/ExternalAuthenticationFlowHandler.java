package com.fjuul.sdk.activitysources.entities;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Handler for the result of connecting to external activity sources. Before calling the {@link #handle(Uri)} method,
 * you should check that the schema of the incoming intent matches the expected for Fjuul SDK.
 */
public final class ExternalAuthenticationFlowHandler {
    /**
     * Determines the status of connecting to the external activity source and returns either it if the data of the
     * intent successfully recognized or null.
     * @param data data of the incoming intent
     * @return connection status
     */
    @Nullable
    public static ConnectionStatus handle(@NonNull Uri data) {
        Objects.requireNonNull(data);
        if (data.getHost() != null && data.getHost().contains("external_connect") &&
            data.getQueryParameterNames().containsAll(Arrays.asList("success", "service"))) {
            final String service = data.getQueryParameter("service");
            final boolean success = data.getBooleanQueryParameter("success", false);
            return new ConnectionStatus(service, success);
        }
        return null;
    }

    public static final class ConnectionStatus {
        @NonNull
        private final String service;
        private final boolean success;

        protected ConnectionStatus(@NonNull String service, boolean success) {
            this.service = service;
            this.success = success;
        }

        /**
         * Returns a string representation of the external activity source being connected that can be matched with
         * {@link ActivitySource.TrackerValue#getValue()}.
         * @return external service
         */
        @NonNull
        public String getService() {
            return service;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}

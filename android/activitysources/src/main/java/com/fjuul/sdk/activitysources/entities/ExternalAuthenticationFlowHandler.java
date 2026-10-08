package com.fjuul.sdk.activitysources.entities;

import java.util.Arrays;
import java.util.Objects;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Handler for the result of connecting to external activity sources. Before calling the {@link #handle(Uri)} method,
 * you should check that the schema of the incoming intent matches the expected for Fjuul SDK.
 */
public final class ExternalAuthenticationFlowHandler {
    /** Known server classifications; unknown errorCode strings remain available to consumers. */
    public static final class ErrorCode {
        public static final String OAUTH_CANCELLED = "oauth_cancelled";
        public static final String GOOGLE_HEALTH_ACCOUNT_NOT_LINKED = "google_health_account_not_linked";

        private ErrorCode() {}
    }

    /**
     * Determines the status of connecting to the external activity source and returns either it if the data of the
     * intent successfully recognized or null.
     *
     * @param data data of the incoming intent
     * @return connection status
     */
    @Nullable
    public static ConnectionStatus handle(@NonNull Uri data) {
        Objects.requireNonNull(data);
        if (data.getHost() != null
            && data.getHost().contains("external_connect")
            && data.getQueryParameterNames().containsAll(Arrays.asList("success", "service"))) {
            final String service = data.getQueryParameter("service");
            final boolean success = data.getBooleanQueryParameter("success", false);
            final String errorCode = data.getQueryParameter("errorCode");
            return new ConnectionStatus(service, success, errorCode);
        }
        return null;
    }

    public static final class ConnectionStatus {
        @NonNull
        private final String service;
        private final boolean success;
        @Nullable
        private final String errorCode;

        protected ConnectionStatus(@NonNull String service, boolean success) {
            this(service, success, null);
        }

        protected ConnectionStatus(@NonNull String service, boolean success, @Nullable String errorCode) {
            this.service = service;
            this.success = success;
            this.errorCode = !success && errorCode != null && !errorCode.isEmpty() ? errorCode : null;
        }

        /**
         * Returns a string representation of the external activity source being connected that can be matched with
         * {@link TrackerValue#getValue()}.
         *
         * @return external service
         */
        @NonNull
        public String getService() {
            return service;
        }

        public boolean isSuccess() {
            return success;
        }

        /**
         * Optional server classification for a failed connection. Missing or empty codes and successful callbacks
         * return null. Unknown codes are preserved.
         *
         * @return error code, or null for success or an unclassified failure
         */
        @Nullable
        public String getErrorCode() {
            return errorCode;
        }
    }
}

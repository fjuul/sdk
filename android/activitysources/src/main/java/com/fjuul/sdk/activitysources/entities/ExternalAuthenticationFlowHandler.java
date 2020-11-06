package com.fjuul.sdk.activitysources.entities;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.Arrays;

public final class ExternalAuthenticationFlowHandler {
    @Nullable
    public static ConnectionStatus handle(Uri data) {
        if (data.getHost() != null && data.getHost().contains("external_connect") &&
            data.getQueryParameterNames().containsAll(Arrays.asList("success", "service"))) {
            final String service = data.getQueryParameter("service");
            final boolean success = data.getBooleanQueryParameter("success", false);
            return new ConnectionStatus(service, success);
        }
        return null;
    }

    public static final class ConnectionStatus {
        private final String service;
        private final boolean success;

        protected ConnectionStatus(String service, boolean success) {
            this.service = service;
            this.success = success;
        }

        public String getService() {
            return service;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}

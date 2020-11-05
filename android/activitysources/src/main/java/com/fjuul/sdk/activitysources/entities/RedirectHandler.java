package com.fjuul.sdk.activitysources.entities;

import android.net.Uri;

import java.util.Arrays;

public final class RedirectHandler {
    public static Redirect process(Uri data) {
        if (data.getHost() != null && data.getHost().contains("external_connect") &&
            data.getQueryParameterNames().containsAll(Arrays.asList("success", "service"))) {
            final String service = data.getQueryParameter("service");
            final boolean success = data.getBooleanQueryParameter("success", false);
            return new ExternalConnectRedirect(service, success);
        }
        return new UnknownRedirect();
    }

    public interface Redirect {}

    public static final class UnknownRedirect implements Redirect {}

    public static final class ExternalConnectRedirect implements Redirect {
        private final String service;
        private final boolean success;

        protected ExternalConnectRedirect(String service, boolean success) {
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

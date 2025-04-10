package com.fjuul.sdk.activitysources.entities;

import java.util.Date;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * A class that encapsulates parameters for syncing sessions from Health Connect. In order to
 * build the instance of this class, use {@link Builder}.
 */
public class HealthConnectSessionSyncOptions {
    @NonNull
    private final Date startDate;
    @NonNull
    private final Date endDate;

    public HealthConnectSessionSyncOptions(@NonNull Date startDate, @NonNull Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    public Date getStartDate() {
        return startDate;
    }

    @NonNull
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Builder of {@link HealthConnectSessionSyncOptions}.
     */
    public static class Builder {
        private Date startDate;
        private Date endDate;

        /**
         * Sets the date range for syncing sessions.
         *
         * @param startDate start date
         * @param endDate end date
         * @return builder
         */
        @NonNull
        public Builder setDateRange(@NonNull Date startDate, @NonNull Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public HealthConnectSessionSyncOptions build() {
            if (startDate == null || endDate == null) {
                throw new IllegalStateException("Date range must be specified");
            }
            if (startDate.after(endDate)) {
                throw new IllegalStateException("Start date must be before or equal to end date");
            }
            return new HealthConnectSessionSyncOptions(startDate, endDate);
        }
    }
} 
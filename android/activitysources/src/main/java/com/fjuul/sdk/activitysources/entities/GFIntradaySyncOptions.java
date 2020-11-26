package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GFIntradaySyncOptions {
    public enum METRICS_TYPE {
        CALORIES,
        STEPS,
        HEART_RATE
    }

    @NonNull private final List<METRICS_TYPE> metrics;
    @NonNull private final LocalDate startDate;
    @NonNull private final LocalDate endDate;

    @NonNull
    public List<METRICS_TYPE> getMetrics() {
        return metrics;
    }

    @NonNull
    public LocalDate getStartDate() {
        return startDate;
    }

    @NonNull
    public LocalDate getEndDate() {
        return endDate;
    }

    private GFIntradaySyncOptions(@NonNull List<METRICS_TYPE> metrics, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        this.metrics = metrics;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    public static class Builder {
        private Set<METRICS_TYPE> metrics = new HashSet<>();
        @Nullable private LocalDate startDate;
        @Nullable private LocalDate endDate;

        public Builder include(@NonNull METRICS_TYPE type) {
            metrics.add(type);
            return this;
        }

        public Builder setDateRange(@NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        @SuppressLint("NewApi")
        public GFIntradaySyncOptions build() {
            if (metrics.isEmpty() || startDate == null || endDate == null) {
                throw new IllegalStateException("Date range and at least one metric type must be specified");
            }
            List<METRICS_TYPE> metricsList = metrics.stream().collect(Collectors.toList());
            return new GFIntradaySyncOptions(metricsList, startDate, endDate);
        }
    }
}

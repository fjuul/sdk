package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GFIntradaySyncOptions {
    public static enum METRICS_TYPE {
        CALORIES,
        STEPS,
        HEART_RATE
    }

    List<METRICS_TYPE> metrics;
    LocalDate startDate;
    LocalDate endDate;

    public List<METRICS_TYPE> getMetrics() {
        return metrics;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    private GFIntradaySyncOptions(List<METRICS_TYPE> metrics, LocalDate startDate, LocalDate endDate) {
        this.metrics = metrics;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    public static class Builder {
        Set<METRICS_TYPE> metrics = new HashSet<>();
        LocalDate startDate;
        LocalDate endDate;

        public Builder include(METRICS_TYPE type) {
            metrics.add(type);
            return this;
        }

        public Builder setDateRange(LocalDate startDate, LocalDate endDate) {
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

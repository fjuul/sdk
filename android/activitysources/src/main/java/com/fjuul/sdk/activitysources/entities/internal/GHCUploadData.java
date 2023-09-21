package com.fjuul.sdk.activitysources.entities.internal;

import java.util.Collections;
import java.util.List;

import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCStepsDataPoint;

import androidx.annotation.NonNull;

public class GHCUploadData {
    @NonNull
    List<GHCCalorieDataPoint> caloriesData = Collections.emptyList();
    @NonNull
    List<GHCStepsDataPoint> stepsData = Collections.emptyList();
    @NonNull
    List<GHCHeartRateSummaryDataPoint> heartRateData = Collections.emptyList();
    @NonNull
    List<GHCSessionBundle> sessionsData = Collections.emptyList();

    @NonNull
    public List<GHCCalorieDataPoint> getCaloriesData() {
        return caloriesData;
    }

    @NonNull
    public List<GHCStepsDataPoint> getStepsData() {
        return stepsData;
    }

    @NonNull
    public List<GHCHeartRateSummaryDataPoint> getHeartRateData() {
        return heartRateData;
    }

    @NonNull
    public List<GHCSessionBundle> getSessionsData() {
        return sessionsData;
    }

    public void setCaloriesData(@NonNull List<GHCCalorieDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    public void setStepsData(@NonNull List<GHCStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    public void setHeartRateData(@NonNull List<GHCHeartRateSummaryDataPoint> heartRateData) {
        this.heartRateData = heartRateData;
    }

    public void setSessionsData(@NonNull List<GHCSessionBundle> sessionsData) {
        this.sessionsData = sessionsData;
    }

    public boolean isEmpty() {
        return caloriesData.isEmpty() && stepsData.isEmpty() && heartRateData.isEmpty() && sessionsData.isEmpty();
    }

    @Override
    public String toString() {
        //@formatter:off
        return "GHCUploadData{" +
            "calories=" + toString(caloriesData) +
            ", steps=" + toString(stepsData) +
            ", heartRates=" + toString(heartRateData) +
            ", sessions=" + toString(sessionsData) +
            '}';
        //@formatter:on
    }

    private static <T> String toString(List<T> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("(count: ");
        builder.append(data.size());
        builder.append("; first elements: ");
        for (int i = 0; i < Math.min(5, data.size()); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(data.get(i));
        }
        builder.append(")");
        return builder.toString();
    }
}

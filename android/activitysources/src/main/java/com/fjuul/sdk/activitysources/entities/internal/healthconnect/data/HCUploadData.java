package com.fjuul.sdk.activitysources.entities.internal.healthconnect.data;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class HCUploadData {
    @NonNull
    List<HCCalorieDataPoint> caloriesData = Collections.emptyList();
    @NonNull
    List<HCStepsDataPoint> stepsData = Collections.emptyList();
    @NonNull
    List<HCHeartRateSummaryDataPoint> heartRateData = Collections.emptyList();

    @NonNull
    public List<HCCalorieDataPoint> getCaloriesData() {
        return caloriesData;
    }

    @NonNull
    public List<HCStepsDataPoint> getStepsData() {
        return stepsData;
    }

    @NonNull
    public List<HCHeartRateSummaryDataPoint> getHeartRateData() {
        return heartRateData;
    }

    public void setCaloriesData(@NonNull List<HCCalorieDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    public void setStepsData(@NonNull List<HCStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    public void setHeartRateData(@NonNull List<HCHeartRateSummaryDataPoint> heartRateData) {
        this.heartRateData = heartRateData;
    }

    public boolean isEmpty() {
        return caloriesData.isEmpty() && stepsData.isEmpty() && heartRateData.isEmpty();
    }

    @Override
    public String toString() {
        //@formatter:off
        return "HCUploadData{" +
            "calories=" + toString(caloriesData) +
            ", steps=" + toString(stepsData) +
            ", heartRates=" + toString(heartRateData) +
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

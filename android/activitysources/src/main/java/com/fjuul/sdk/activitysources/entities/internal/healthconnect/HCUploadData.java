package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

public class HCUploadData {
    private List<HCStepsDataPoint> stepsData = new ArrayList<>();
    private List<HCCaloriesDataPoint> caloriesData = new ArrayList<>();
    private List<HCHeartRateDataPoint> heartRateData = new ArrayList<>();
    private List<HCWeightDataPoint> weightData = new ArrayList<>();
    private List<HCHeightDataPoint> heightData = new ArrayList<>();

    @NonNull
    public List<HCStepsDataPoint> getStepsData() {
        return stepsData;
    }

    public void setStepsData(@NonNull List<HCStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    @NonNull
    public List<HCCaloriesDataPoint> getCaloriesData() {
        return caloriesData;
    }

    public void setCaloriesData(@NonNull List<HCCaloriesDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    @NonNull
    public List<HCHeartRateDataPoint> getHeartRateData() {
        return heartRateData;
    }

    public void setHeartRateData(@NonNull List<HCHeartRateDataPoint> heartRateData) {
        this.heartRateData = heartRateData;
    }

    @NonNull
    public List<HCHeartRateDataPoint> getIntradayHeartRateData() {
        return heartRateData.stream()
            .filter(HCHeartRateDataPoint::isIntraday)
            .collect(Collectors.toList());
    }

    @NonNull
    public List<HCHeartRateDataPoint> getRestingHeartRateData() {
        return heartRateData.stream()
            .filter(HCHeartRateDataPoint::isResting)
            .collect(Collectors.toList());
    }

    @NonNull
    public List<HCWeightDataPoint> getWeightData() {
        return weightData;
    }

    public void setWeightData(@NonNull List<HCWeightDataPoint> weightData) {
        this.weightData = weightData;
    }

    @NonNull
    public List<HCHeightDataPoint> getHeightData() {
        return heightData;
    }

    public void setHeightData(@NonNull List<HCHeightDataPoint> heightData) {
        this.heightData = heightData;
    }

    public boolean isEmpty() {
        return stepsData.isEmpty() &&
            caloriesData.isEmpty() &&
            heartRateData.isEmpty() &&
            weightData.isEmpty() &&
            heightData.isEmpty();
    }
} 
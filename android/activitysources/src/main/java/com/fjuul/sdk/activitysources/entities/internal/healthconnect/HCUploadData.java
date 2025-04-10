package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class HCUploadData {
    @NonNull
    private final List<HCStepsDataPoint> stepsData;
    @NonNull
    private final List<HCCaloriesDataPoint> caloriesData;
    @NonNull
    private final List<HCIntradayHeartRateDataPoint> intradayHeartRateData;
    @NonNull
    private final List<HCRestingHeartRateDataPoint> restingHeartRateData;
    @NonNull
    private final List<HCWeightDataPoint> weightData;
    @NonNull
    private final List<HCHeightDataPoint> heightData;

    public HCUploadData() {
        this.stepsData = new ArrayList<>();
        this.caloriesData = new ArrayList<>();
        this.intradayHeartRateData = new ArrayList<>();
        this.restingHeartRateData = new ArrayList<>();
        this.weightData = new ArrayList<>();
        this.heightData = new ArrayList<>();
    }

    @NonNull
    public List<HCStepsDataPoint> getStepsData() {
        return stepsData;
    }

    public void setStepsData(@NonNull List<HCStepsDataPoint> stepsData) {
        this.stepsData.clear();
        this.stepsData.addAll(stepsData);
    }

    @NonNull
    public List<HCCaloriesDataPoint> getCaloriesData() {
        return caloriesData;
    }

    public void setCaloriesData(@NonNull List<HCCaloriesDataPoint> caloriesData) {
        this.caloriesData.clear();
        this.caloriesData.addAll(caloriesData);
    }

    @NonNull
    public List<HCIntradayHeartRateDataPoint> getIntradayHeartRateData() {
        return intradayHeartRateData;
    }

    public void setIntradayHeartRateData(@NonNull List<HCIntradayHeartRateDataPoint> intradayHeartRateData) {
        this.intradayHeartRateData.clear();
        this.intradayHeartRateData.addAll(intradayHeartRateData);
    }

    @NonNull
    public List<HCRestingHeartRateDataPoint> getRestingHeartRateData() {
        return restingHeartRateData;
    }

    public void setRestingHeartRateData(@NonNull List<HCRestingHeartRateDataPoint> restingHeartRateData) {
        this.restingHeartRateData.clear();
        this.restingHeartRateData.addAll(restingHeartRateData);
    }

    @NonNull
    public List<HCWeightDataPoint> getWeightData() {
        return weightData;
    }

    public void setWeightData(@NonNull List<HCWeightDataPoint> weightData) {
        this.weightData.clear();
        this.weightData.addAll(weightData);
    }

    @NonNull
    public List<HCHeightDataPoint> getHeightData() {
        return heightData;
    }

    public void setHeightData(@NonNull List<HCHeightDataPoint> heightData) {
        this.heightData.clear();
        this.heightData.addAll(heightData);
    }

    public boolean isEmpty() {
        return stepsData.isEmpty() &&
            caloriesData.isEmpty() &&
            intradayHeartRateData.isEmpty() &&
            restingHeartRateData.isEmpty() &&
            weightData.isEmpty() &&
            heightData.isEmpty();
    }

    public void merge(@NonNull HCUploadData other) {
        // Merge steps data by date
        stepsData.clear();
        stepsData.addAll(other.stepsData);
        // Merge calories data by date
        caloriesData.clear();
        caloriesData.addAll(other.caloriesData);
        // Merge heart rate data by date
        intradayHeartRateData.clear();
        intradayHeartRateData.addAll(other.intradayHeartRateData);
        restingHeartRateData.clear();
        restingHeartRateData.addAll(other.restingHeartRateData);
        // Merge weight data by date
        weightData.clear();
        weightData.addAll(other.weightData);
        // Merge height data by date
        heightData.clear();
        heightData.addAll(other.heightData);
    }
} 
package com.fjuul.sdk.activitysources.entities.internal;

import androidx.annotation.NonNull;

import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCStepsDataPoint;

import java.util.Collections;
import java.util.List;

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
            "calories=" + caloriesData.size() +
            ", steps=" + stepsData.size() +
            ", heartRates=" + heartRateData.size() +
            ", sessions=" + sessionsData.size() +
            '}';
        //@formatter:on
    }
}

package com.fjuul.sdk.activitysources.entities.internal;

import androidx.annotation.NonNull;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;

import java.util.Collections;
import java.util.List;

public class GFUploadData {
    @NonNull List<GFCalorieDataPoint> caloriesData = Collections.emptyList();
    @NonNull List<GFStepsDataPoint> stepsData = Collections.emptyList();
    @NonNull List<GFHRSummaryDataPoint> hrData = Collections.emptyList();
    @NonNull List<GFSessionBundle> sessionsData = Collections.emptyList();

    @NonNull public List<GFCalorieDataPoint> getCaloriesData() {
        return caloriesData;
    }

    @NonNull public List<GFStepsDataPoint> getStepsData() {
        return stepsData;
    }

    @NonNull public List<GFHRSummaryDataPoint> getHrData() {
        return hrData;
    }

    @NonNull public List<GFSessionBundle> getSessionsData() {
        return sessionsData;
    }

    public void setCaloriesData(@NonNull List<GFCalorieDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    public void setStepsData(@NonNull List<GFStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    public void setHrData(@NonNull List<GFHRSummaryDataPoint> hrData) {
        this.hrData = hrData;
    }

    public void setSessionsData(@NonNull List<GFSessionBundle> sessionsData) {
        this.sessionsData = sessionsData;
    }

    public boolean isEmpty() {
        return caloriesData.isEmpty() && stepsData.isEmpty() && hrData.isEmpty() && sessionsData.isEmpty();
    }
}

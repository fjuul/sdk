package com.fjuul.sdk.activitysources.entities;

import java.util.Collections;
import java.util.List;

public class GFUploadData {
    List<GFCalorieDataPoint> caloriesData = Collections.emptyList();
    List<GFStepsDataPoint> stepsData = Collections.emptyList();
    List<GFHRSummaryDataPoint> hrData = Collections.emptyList();
    List<GFSessionBundle> sessionsData = Collections.emptyList();

    public void setCaloriesData(List<GFCalorieDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    public void setStepsData(List<GFStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    public void setHrData(List<GFHRSummaryDataPoint> hrData) {
        this.hrData = hrData;
    }

    public void setSessionsData(List<GFSessionBundle> sessionsData) {
        this.sessionsData = sessionsData;
    }
}

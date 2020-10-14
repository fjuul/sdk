package com.fjuul.sdk.activitysources.entities;

import java.util.Collections;
import java.util.List;

class GFUploadData {
    List<GFCalorieDataPoint> caloriesData = Collections.emptyList();
    List<GFStepsDataPoint> stepsData = Collections.emptyList();
    List<GFHRDataPoint> hrData = Collections.emptyList();
    List<GFSessionBundle> sessionsData = Collections.emptyList();

    public void setCaloriesData(List<GFCalorieDataPoint> caloriesData) {
        this.caloriesData = caloriesData;
    }

    public void setStepsData(List<GFStepsDataPoint> stepsData) {
        this.stepsData = stepsData;
    }

    public void setHrData(List<GFHRDataPoint> hrData) {
        this.hrData = hrData;
    }

    public void setSessionsData(List<GFSessionBundle> sessionsData) {
        this.sessionsData = sessionsData;
    }
}

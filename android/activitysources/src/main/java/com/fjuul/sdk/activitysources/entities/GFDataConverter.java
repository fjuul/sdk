package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GFDataConverter {
    private static final String TAG = "GFDataConverter";

    public static Task<List<GFCalorieDataPoint>> convertDataReadResponseToCalories(DataReadResponse dataReadResponse) {
        ArrayList<GFCalorieDataPoint> calorieDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
            DataPoint calorieDataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = calorieDataPoint.getOriginalDataSource().getStreamIdentifier();
            for (Field field : DataType.TYPE_CALORIES_EXPENDED.getFields()) {
                if (Field.FIELD_CALORIES.equals(field)) {
                    float kcals = calorieDataPoint.getValue(field).asFloat();
                    GFCalorieDataPoint calorie = new GFCalorieDataPoint(kcals, start, dataSourceId);
                    calorieDataPoints.add(calorie);
                }
            }
        }
        return Tasks.forResult(calorieDataPoints);
    }

    public static GFCalorieDataPoint convertDataPointToCalorie(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_CALORIES_EXPENDED.getFields()) {
            if (Field.FIELD_CALORIES.equals(field)) {
                float kcals = dataPoint.getValue(field).asFloat();
                return new GFCalorieDataPoint(kcals, start, end, dataSourceId);
            }
        }
        return null;
    }

    public static Task<List<GFStepsDataPoint>> convertDataReadResponseToSteps(DataReadResponse dataReadResponse) {
        ArrayList<GFStepsDataPoint> stepsDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
            DataPoint stepsDataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = stepsDataPoint.getOriginalDataSource().getStreamIdentifier();
            for (Field field : DataType.TYPE_STEP_COUNT_DELTA.getFields()) {
                if (Field.FIELD_STEPS.equals(field)) {
                    int steps = stepsDataPoint.getValue(field).asInt();
                    GFStepsDataPoint convertedStepsDataPoint = new GFStepsDataPoint(steps, start, dataSourceId);
                    stepsDataPoints.add(convertedStepsDataPoint);
                }
            }
        }
        return Tasks.forResult(stepsDataPoints);
    }

    public static GFStepsDataPoint convertDataPointToSteps(DataPoint dataPoint) {
        Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        for (Field field : DataType.TYPE_STEP_COUNT_DELTA.getFields()) {
            if (Field.FIELD_STEPS.equals(field)) {
                int steps = dataPoint.getValue(field).asInt();
                return new GFStepsDataPoint(steps, start, end, dataSourceId);
            }
        }
        return null;
    }

    public static Task<List<GFHRSummaryDataPoint>> convertDataReadResponseToHRSummaries(DataReadResponse dataReadResponse) {
        ArrayList<GFHRSummaryDataPoint> hrDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
            DataPoint dataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
            float avgBPM = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat();
            float minBPM = dataPoint.getValue(Field.FIELD_MIN).asFloat();
            float maxBPM = dataPoint.getValue(Field.FIELD_MAX).asFloat();
            GFHRSummaryDataPoint hrSummary = new GFHRSummaryDataPoint(avgBPM, minBPM, maxBPM, start, dataSourceId);
            hrDataPoints.add(hrSummary);
        }
        return Tasks.forResult(hrDataPoints);
    }

    public static GFHRDataPoint convertDataPointToHR(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_HEART_RATE_BPM.getFields()) {
            if (Field.FIELD_BPM.equals(field)) {
                float bpm = dataPoint.getValue(field).asFloat();
                return new GFHRDataPoint(bpm, start, dataSourceId);
            }
        }
        return null;
    }

    public static GFPowerDataPoint convertDataPointToPower(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_POWER_SAMPLE.getFields()) {
            if (Field.FIELD_WATTS.equals(field)) {
                float watts = dataPoint.getValue(field).asFloat();
                return new GFPowerDataPoint(watts, start, dataSourceId);
            }
        }
        return null;
    }

    public static GFActivitySegmentDataPoint convertDataPointToActivitySegment(DataPoint dataPoint) {
        final String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        final Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        final Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        final int activityType = dataPoint.getValue(Field.FIELD_ACTIVITY).asInt();
        return new GFActivitySegmentDataPoint(activityType, start,end,dataSourceId);
    }

    public static GFSpeedDataPoint convertDataPointToSpeed(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_SPEED.getFields()) {
            if (Field.FIELD_SPEED.equals(field)) {
                float metersPerSecond = dataPoint.getValue(field).asFloat();
                return new GFSpeedDataPoint(metersPerSecond, start, dataSourceId);
            }
        }
        return null;
    }
}

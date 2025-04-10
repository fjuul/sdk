package com.fjuul.sdk.activitysources.utils.healthconnect;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCWeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCHeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCSessionBundle;

import androidx.health.connect.client.aggregate.AggregationResult;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord;

public class HCDataUtils {
    public static List<HCDataPoint> convertAggregationResultToDataPoints(AggregationResult result, String metricType) {
        List<HCDataPoint> dataPoints = new ArrayList<>();
        
        for (Instant time : result.getTimePoints()) {
            Double value = result.get(time);
            if (value != null) {
                HCDataPoint dataPoint = createDataPoint(metricType, time, value);
                if (dataPoint != null) {
                    dataPoints.add(dataPoint);
                }
            }
        }
        
        return dataPoints;
    }
    
    public static List<HCSessionBundle> convertExerciseSessions(List<ExerciseSessionRecord> sessions) {
        List<HCSessionBundle> bundles = new ArrayList<>();
        
        for (ExerciseSessionRecord session : sessions) {
            HCSessionBundle bundle = new HCSessionBundle(
                session.getStartTime().toEpochMilli(),
                session.getEndTime().toEpochMilli(),
                session.getExerciseType().toString(),
                session.getTitle(),
                session.getNotes()
            );
            bundles.add(bundle);
        }
        
        return bundles;
    }
    
    public static List<HCDataPoint> convertWeightRecords(List<WeightRecord> records) {
        List<HCDataPoint> dataPoints = new ArrayList<>();
        
        for (WeightRecord record : records) {
            HCWeightDataPoint dataPoint = new HCWeightDataPoint(
                record.getTime().toEpochMilli(),
                record.getWeight().getKilograms()
            );
            dataPoints.add(dataPoint);
        }
        
        return dataPoints;
    }
    
    public static List<HCDataPoint> convertHeightRecords(List<HeightRecord> records) {
        List<HCDataPoint> dataPoints = new ArrayList<>();
        
        for (HeightRecord record : records) {
            HCHeightDataPoint dataPoint = new HCHeightDataPoint(
                record.getTime().toEpochMilli(),
                record.getHeight().getMeters()
            );
            dataPoints.add(dataPoint);
        }
        
        return dataPoints;
    }
    
    private static HCDataPoint createDataPoint(String metricType, Instant time, Double value) {
        switch (metricType) {
            case "calories":
                return new HCCalorieDataPoint(time.toEpochMilli(), value);
            case "steps":
                return new HCStepsDataPoint(time.toEpochMilli(), value);
            case "heart_rate":
                return new HCHRSummaryDataPoint(time.toEpochMilli(), value);
            default:
                return null;
        }
    }
} 
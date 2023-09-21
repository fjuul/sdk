package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fjuul.sdk.activitysources.entities.internal.ExerciseSession;

import androidx.annotation.NonNull;
import androidx.health.connect.client.records.ExerciseSegment;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.PowerRecord;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.WeightRecord;

public class GHCDataConverter {
    @NonNull
    public static GHCCalorieDataPoint convertRecordToCalories(@NonNull TotalCaloriesBurnedRecord record) {
        final Date start = Date.from(record.getStartTime());
        final double kcals = record.getEnergy().getCalories();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCCalorieDataPoint((float) kcals, start, dataSourceId);
    }

    @NonNull
    public static GHCStepsDataPoint convertRecordToSteps(@NonNull StepsRecord record) {
        final Date start = Date.from(record.getStartTime());
        final long steps = record.getCount();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCStepsDataPoint((int) steps, start, dataSourceId);
    }

    @NonNull
    public static GHCPowerSummaryDataPoint convertRecordToPowerSummary(@NonNull PowerRecord record) {
        final Date start = Date.from(record.getStartTime());
        final Date end = Date.from(record.getEndTime());
        double total = 0;
        double min = Float.MAX_VALUE;
        double max = Float.MIN_VALUE;
        for (PowerRecord.Sample sample : record.getSamples()) {
            double bpm = sample.getPower().getWatts();
            total += bpm;
            if (bpm < min) {
                min = bpm;
            }
            if (bpm > max) {
                max = bpm;
            }
        }
        double avg = total / record.getSamples().size();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCPowerSummaryDataPoint(avg, min, max, start, end, dataSourceId);
    }

    @NonNull
    public static GHCHeartRateSummaryDataPoint convertRecordToHeartRateSummary(@NonNull HeartRateRecord record) {
        final Date start = Date.from(record.getStartTime());
        final Date end = Date.from(record.getEndTime());
        float total = 0;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (HeartRateRecord.Sample sample : record.getSamples()) {
            float bpm = sample.getBeatsPerMinute();
            total += bpm;
            if (bpm < min) {
                min = bpm;
            }
            if (bpm > max) {
                max = bpm;
            }
        }
        float avg = total / record.getSamples().size();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCHeartRateSummaryDataPoint(avg, min, max, start, end, dataSourceId);
    }

    @NonNull
    public static GHCSpeedSummaryDataPoint convertRecordToSpeedSummary(@NonNull SpeedRecord record) {
        final Date start = Date.from(record.getStartTime());
        final Date end = Date.from(record.getEndTime());
        double total = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        // It's slightly unreasonable to calculate average speed as just the average of the
        // samples, without e.g. looking at the duration of each sample, but let's keep
        // it simple.
        for (SpeedRecord.Sample sample : record.getSamples()) {
            double speed = sample.getSpeed().getMetersPerSecond();
            total += speed;
            if (speed < min) {
                min = speed;
            }
            if (speed > max) {
                max = speed;
            }
        }
        double avg = total / record.getSamples().size();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCSpeedSummaryDataPoint(avg, min, max, start, end, dataSourceId);
    }

    @NonNull
    public static GHCHeightDataPoint convertRecordToHeight(@NonNull HeightRecord record) {
        final Date start = Date.from(record.getTime());
        final double height = record.getHeight().getMeters();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCHeightDataPoint((float) height, start, dataSourceId);
    }

    @NonNull
    public static GHCWeightDataPoint convertRecordToWeight(@NonNull WeightRecord record) {
        final Date start = Date.from(record.getTime());
        final double weight = record.getWeight().getKilograms();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new GHCWeightDataPoint((float) weight, start, dataSourceId);
    }

    @NonNull
    public static GHCSessionBundle convertSessionToSessionBundle(@NonNull ExerciseSession session) {
        final ExerciseSessionRecord sessionRecord = session.getSessionRecord();
        final String title = sessionRecord.getTitle();
        final String notes = sessionRecord.getNotes();
        final Date start = Date.from(sessionRecord.getStartTime());
        final Date end = Date.from(sessionRecord.getEndTime());
        final int type = sessionRecord.getExerciseType();
        final List<GHCActivitySegmentDataPoint> activitySegmentDataPoints = new ArrayList<>();
        // Using Stream::toList seems to require API level 34, so doing it the long way
        for (ExerciseSegment segment : sessionRecord.getSegments()) {
            activitySegmentDataPoints.add(convertSegment(segment));
        }
        final List<GHCCalorieDataPoint> calorieDataPoints = new ArrayList<>();
        for (TotalCaloriesBurnedRecord record : session.getCaloriesRecords()) {
            calorieDataPoints.add(convertRecordToCalories(record));
        }
        final List<GHCStepsDataPoint> stepsDataPoints = new ArrayList<>();
        for (StepsRecord record : session.getStepsRecords()) {
            stepsDataPoints.add(convertRecordToSteps(record));
        }
        final List<GHCHeartRateSummaryDataPoint> heartRateDataPoints = new ArrayList<>();
        for (HeartRateRecord record : session.getHeartRateRecords()) {
            heartRateDataPoints.add(convertRecordToHeartRateSummary(record));
        }
        final List<GHCPowerSummaryDataPoint> powerDataPoints = new ArrayList<>();
        for (PowerRecord record : session.getPowerRecords()) {
            powerDataPoints.add(convertRecordToPowerSummary(record));
        }
        final List<GHCSpeedSummaryDataPoint> speedDataPoints = new ArrayList<>();
        for (SpeedRecord record : session.getSpeedRecords()) {
            speedDataPoints.add(convertRecordToSpeedSummary(record));
        }
        return new GHCSessionBundle(title,
            notes,
            start,
            end,
            type,
            activitySegmentDataPoints,
            calorieDataPoints,
            stepsDataPoints,
            heartRateDataPoints,
            powerDataPoints,
            speedDataPoints);
    }

    @NonNull
    private static GHCActivitySegmentDataPoint convertSegment(@NonNull ExerciseSegment segment) {
        Date startTime = Date.from(segment.getStartTime());
        Date endTime = Date.from(segment.getEndTime());
        int segmentType = segment.getSegmentType();
        return new GHCActivitySegmentDataPoint(segmentType, startTime, endTime);
    }
}

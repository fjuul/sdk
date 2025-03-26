package com.fjuul.sdk.activitysources.entities.internal.healthconnect.data;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;

public class HCDataConverter {
    @NonNull
    public static HCCalorieDataPoint convertRecordToCalories(@NonNull TotalCaloriesBurnedRecord record) {
        final Date start = Date.from(record.getStartTime());
        final Date end = Date.from(record.getEndTime());
        final double kcals = record.getEnergy().getKilocalories();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new HCCalorieDataPoint((float) kcals, start, end, dataSourceId);
    }

    @NonNull
    public static HCStepsDataPoint convertRecordToSteps(@NonNull StepsRecord record) {
        final Date start = Date.from(record.getStartTime());
        final Date end = Date.from(record.getEndTime());
        final long steps = record.getCount();
        final String dataSourceId = record.getMetadata().getDataOrigin().getPackageName();
        return new HCStepsDataPoint((int) steps, start, end, dataSourceId);
    }

    @NonNull
    public static HCHeartRateSummaryDataPoint convertRecordToHeartRateSummary(@NonNull HeartRateRecord record) {
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
        return new HCHeartRateSummaryDataPoint(avg, min, max, start, end, dataSourceId);
    }

}

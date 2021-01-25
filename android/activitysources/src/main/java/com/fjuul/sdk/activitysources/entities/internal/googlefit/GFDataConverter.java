package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GFDataConverter {
    private static final String TAG = "GFDataConverter";

    @Nullable
    public static GFCalorieDataPoint convertBucketToCalorie(@NonNull Bucket bucket) {
        final Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
        if (bucket.getDataSets().isEmpty()) {
            return null;
        }
        final DataSet dataSet = bucket.getDataSets().get(0);
        if (dataSet.isEmpty() || !dataSet.getDataType().equals(DataType.AGGREGATE_CALORIES_EXPENDED)) {
            return null;
        }
        final DataPoint calorieDataPoint = dataSet.getDataPoints().get(0);
        final String dataSourceId = tryToExtractDataSourceStreamId(calorieDataPoint);
        float kcals = calorieDataPoint.getValue(Field.FIELD_CALORIES).asFloat();
        return new GFCalorieDataPoint(kcals, start, dataSourceId);
    }

    @NonNull
    public static GFCalorieDataPoint convertDataPointToCalorie(@NonNull DataPoint dataPoint) {
        final String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        final Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        final Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        float kcals = dataPoint.getValue(Field.FIELD_CALORIES).asFloat();
        return new GFCalorieDataPoint(kcals, start, end, dataSourceId);
    }

    @Nullable
    public static GFStepsDataPoint convertBucketToSteps(@NonNull Bucket bucket) {
        final Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
        if (bucket.getDataSets().isEmpty()) {
            return null;
        }
        final DataSet dataSet = bucket.getDataSets().get(0);
        if (dataSet.isEmpty()) {
            return null;
        }
        final DataPoint stepsDataPoint = dataSet.getDataPoints().get(0);
        final String dataSourceId = tryToExtractDataSourceStreamId(stepsDataPoint);
        int steps = stepsDataPoint.getValue(Field.FIELD_STEPS).asInt();
        return new GFStepsDataPoint(steps, start, dataSourceId);
    }

    @NonNull
    public static GFStepsDataPoint convertDataPointToSteps(@NonNull DataPoint dataPoint) {
        final Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        final Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        final String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        int steps = dataPoint.getValue(Field.FIELD_STEPS).asInt();
        return new GFStepsDataPoint(steps, start, end, dataSourceId);
    }

    @Nullable
    public static GFHRSummaryDataPoint convertBucketToHRSummary(@NonNull Bucket bucket) {
        Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
        if (bucket.getDataSets().isEmpty()) {
            return null;
        }
        DataSet dataSet = bucket.getDataSets().get(0);
        if (dataSet.isEmpty()) {
            return null;
        }
        DataPoint dataPoint = dataSet.getDataPoints().get(0);
        String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        float avgBPM = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat();
        float minBPM = dataPoint.getValue(Field.FIELD_MIN).asFloat();
        float maxBPM = dataPoint.getValue(Field.FIELD_MAX).asFloat();
        return new GFHRSummaryDataPoint(avgBPM, minBPM, maxBPM, start, dataSourceId);
    }

    @NonNull
    public static GFHRDataPoint convertDataPointToHR(@NonNull DataPoint dataPoint) {
        String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        float bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat();
        return new GFHRDataPoint(bpm, start, dataSourceId);
    }

    @NonNull
    public static GFPowerDataPoint convertDataPointToPower(@NonNull DataPoint dataPoint) {
        String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        float watts = dataPoint.getValue(Field.FIELD_WATTS).asFloat();
        return new GFPowerDataPoint(watts, start, dataSourceId);
    }

    @NonNull
    public static GFActivitySegmentDataPoint convertDataPointToActivitySegment(@NonNull DataPoint dataPoint) {
        final String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        final Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        final Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        final int activityType = dataPoint.getValue(Field.FIELD_ACTIVITY).asInt();
        return new GFActivitySegmentDataPoint(activityType, start, end, dataSourceId);
    }

    @NonNull
    public static GFSpeedDataPoint convertDataPointToSpeed(@NonNull DataPoint dataPoint) {
        String dataSourceId = tryToExtractDataSourceStreamId(dataPoint);
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        float metersPerSecond = dataPoint.getValue(Field.FIELD_SPEED).asFloat();
        return new GFSpeedDataPoint(metersPerSecond, start, dataSourceId);
    }

    @SuppressLint("NewApi")
    @Nullable
    private static String tryToExtractDataSourceStreamId(@NonNull DataPoint point) {
        final DataSource dataSource = point.getOriginalDataSource();
        if (dataSource == null) {
            return null;
        }
        String type = "unknown";
        switch (dataSource.getType()) {
            case DataSource.TYPE_RAW:
                type = "raw";
                break;
            case DataSource.TYPE_DERIVED:
                type = "derived";
                break;
        }
        final StringBuilder dataSourceIdentifierBuilder = new StringBuilder();
        dataSourceIdentifierBuilder.append(type).append(":").append(dataSource.getDataType().getName());
        final String packageName = dataSource.getAppPackageName();
        if (!isNullOrEmptyString(packageName)) {
            dataSourceIdentifierBuilder.append(":").append(packageName);
        }
        final Device sourceDevice = dataSource.getDevice();
        if (sourceDevice != null) {
            final String deviceInfo =
                Stream.of(sourceDevice.getManufacturer(), sourceDevice.getModel(), sourceDevice.getUid())
                    .filter(value -> !isNullOrEmptyString(value))
                    .collect(Collectors.joining(":"));
            if (!isNullOrEmptyString(deviceInfo)) {
                dataSourceIdentifierBuilder.append(":").append(deviceInfo);
            }
        }
        return dataSourceIdentifierBuilder.toString();
    }

    @SuppressLint("NewApi")
    private static boolean isNullOrEmptyString(@Nullable String string) {
        return Objects.isNull(string) || string.isEmpty();
    }
}

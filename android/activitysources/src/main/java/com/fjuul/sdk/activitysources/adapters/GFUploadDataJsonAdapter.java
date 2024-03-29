package com.fjuul.sdk.activitysources.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.internal.GFUploadData;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFScalarDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFInstantMeasureSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFIntradayHRSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFIntradaySampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSampleJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSessionJson;
import com.squareup.moshi.ToJson;

import android.annotation.SuppressLint;

public class GFUploadDataJsonAdapter {
    @SuppressLint("NewApi")
    @ToJson
    GFUploadDataJson toJson(GFUploadData uploadData) {
        final List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData =
            groupAndMapGFPointsToJsonSample(uploadData.getCaloriesData(), this::mapDataPointToIntradayEntry);
        final List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData =
            groupAndMapGFPointsToJsonSample(uploadData.getStepsData(), this::mapDataPointToIntradayEntry);
        final List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData =
            groupAndMapGFPointsToJsonSample(uploadData.getHrData(), this::mapDataPointToIntradayHREntry);
        final List<GFSessionJson> sessionsData =
            uploadData.getSessionsData().stream().map(this::mapSessionBundleToJson).collect(Collectors.toList());
        return new GFUploadDataJson(caloriesData, stepsData, hrData, sessionsData);
    }

    @SuppressLint("NewApi")
    private <T extends GFDataPoint, O> List<GFSampleJson<O>> groupAndMapGFPointsToJsonSample(List<T> dataPoints,
        Function<T, O> pointMapper) {
        final Map<Optional<String>, List<T>> groupedByDataSource =
            dataPoints.stream().collect(Collectors.groupingBy(point -> Optional.ofNullable(point.getDataSource())));
        final List<GFSampleJson<O>> samples = groupedByDataSource.entrySet().stream().map((entry) -> {
            final String dataSource = entry.getKey().orElse(null);
            List<O> entries = entry.getValue().stream().map(pointMapper).collect(Collectors.toList());
            return new GFSampleJson<>(dataSource, entries);
        }).collect(Collectors.toList());
        return samples;
    }

    private GFSessionJson mapSessionBundleToJson(GFSessionBundle sessionBundle) {
        final List<GFSampleJson<GFSampleEntryJson<Integer>>> activitySegmentsData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getActivitySegments(), this::mapDataPointToSampleEntry);
        final List<GFSampleJson<GFSampleEntryJson<Float>>> caloriesData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getCalories(), this::mapDataPointToSampleEntry);
        final List<GFSampleJson<GFSampleEntryJson<Integer>>> stepsData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getSteps(), this::mapDataPointToSampleEntry);
        final List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speedData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getSpeed(), this::mapDataPointToInstantMeasureSampleEntry);
        final List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> hrData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getHeartRate(),
                this::mapDataPointToInstantMeasureSampleEntry);
        final List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> powerData =
            groupAndMapGFPointsToJsonSample(sessionBundle.getPower(), this::mapDataPointToInstantMeasureSampleEntry);
        return new GFSessionJson(sessionBundle.getId(),
            sessionBundle.getName(),
            sessionBundle.getApplicationIdentifier(),
            sessionBundle.getTimeStart(),
            sessionBundle.getTimeEnd(),
            sessionBundle.getType(),
            activitySegmentsData,
            caloriesData,
            stepsData,
            speedData,
            hrData,
            powerData);
    }

    private <T extends Number> GFIntradaySampleEntryJson<T> mapDataPointToIntradayEntry(GFScalarDataPoint<T> point) {
        return new GFIntradaySampleEntryJson<>(point.getStart(), point.getValue());
    }

    private GFIntradayHRSampleEntryJson mapDataPointToIntradayHREntry(GFHRSummaryDataPoint point) {
        return new GFIntradayHRSampleEntryJson(point.getStart(), point.getAvg(), point.getMin(), point.getMax());
    }

    private <T extends Number> GFSampleEntryJson<T> mapDataPointToSampleEntry(GFScalarDataPoint<T> point) {
        if (point.getEnd() == null) {
            throw new IllegalStateException("GFScalarDataPoint must have the defined end time: " + point.toString());
        }
        return new GFSampleEntryJson<>(point.getStart(), point.getEnd(), point.getValue());
    }

    private <T extends Number> GFInstantMeasureSampleEntryJson<T> mapDataPointToInstantMeasureSampleEntry(
        GFScalarDataPoint<T> point) {
        return new GFInstantMeasureSampleEntryJson<>(point.getStart(), point.getValue());
    }
}

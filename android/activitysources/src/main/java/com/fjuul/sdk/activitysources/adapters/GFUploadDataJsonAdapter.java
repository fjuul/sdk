package com.fjuul.sdk.activitysources.adapters;

import android.annotation.SuppressLint;

import com.fjuul.sdk.activitysources.entities.GFDataPoint;
import com.fjuul.sdk.activitysources.entities.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.GFScalarDataPoint;
import com.fjuul.sdk.activitysources.entities.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.GFUploadData;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFInstantMeasureSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFIntradayHRSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFIntradaySampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSampleJson;
import com.fjuul.sdk.activitysources.json.GFUploadDataJson.GFSessionJson;
import com.squareup.moshi.ToJson;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GFUploadDataJsonAdapter {
    @SuppressLint("NewApi")
    @ToJson
    GFUploadDataJson toJson(GFUploadData uploadData) {
        List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData = groupAndMapGFPointsToJsonSample(uploadData.getCaloriesData(), this::mapDataPointToIntradayEntry);
        List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData = groupAndMapGFPointsToJsonSample(uploadData.getStepsData(), this::mapDataPointToIntradayEntry);
        List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData = groupAndMapGFPointsToJsonSample(uploadData.getHrData(), this::mapDataPointToIntradayHREntry);
        List<GFSessionJson> sessionsData = uploadData.getSessionsData().stream().map(this::mapSessionBundleToJson).collect(Collectors.toList());
        return new GFUploadDataJson(caloriesData, stepsData, hrData, sessionsData);
    }

    @SuppressLint("NewApi")
    private <T extends GFDataPoint, O> List<GFSampleJson<O>> groupAndMapGFPointsToJsonSample(List<T> dataPoints, Function<T, O> pointMapper) {
        Map<String, List<T>> groupedByDataSource = dataPoints.stream().collect(Collectors.groupingBy(T::getDataSource));
        List<GFSampleJson<O>> samples = groupedByDataSource.entrySet().stream().map((entry) -> {
            String dataSource = entry.getKey();
            List<O> entries = entry.getValue().stream().map(pointMapper).collect(Collectors.toList());
            return new GFSampleJson<>(dataSource, entries);
        }).collect(Collectors.toList());
        return samples;
    }

    private GFSessionJson mapSessionBundleToJson(GFSessionBundle sessionBundle) {
        List<GFSampleJson<GFSampleEntryJson<Float>>> caloriesData = groupAndMapGFPointsToJsonSample(sessionBundle.getCalories(), this::mapDataPointToSampleEntry);
        List<GFSampleJson<GFSampleEntryJson<Integer>>> stepsData = groupAndMapGFPointsToJsonSample(sessionBundle.getSteps(), this::mapDataPointToSampleEntry);
        List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speedData = groupAndMapGFPointsToJsonSample(sessionBundle.getSpeed(), this::mapDataPointToInstantMeasureSampleEntry);
        List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> hrData = groupAndMapGFPointsToJsonSample(sessionBundle.getHeartRate(), this::mapDataPointToInstantMeasureSampleEntry);
        List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> powerData = groupAndMapGFPointsToJsonSample(sessionBundle.getPower(), this::mapDataPointToInstantMeasureSampleEntry);
        return new GFSessionJson(
            sessionBundle.getId(),
            sessionBundle.getName(),
            sessionBundle.getApplicationIdentifier(),
            sessionBundle.getTimeStart(),
            sessionBundle.getTimeEnd(),
            sessionBundle.getType(),
            caloriesData,
            stepsData,
            speedData,
            hrData,
            powerData
        );
    }

    private <T extends Number> GFIntradaySampleEntryJson<T> mapDataPointToIntradayEntry(GFScalarDataPoint<T> point) {
        return new GFIntradaySampleEntryJson<>(point.getStart(), point.getValue());
    }

    private GFIntradayHRSampleEntryJson mapDataPointToIntradayHREntry(GFHRSummaryDataPoint point) {
        return new GFIntradayHRSampleEntryJson(point.getStart(), point.getAvg(), point.getMin(), point.getMax());
    }

    private <T extends Number> GFSampleEntryJson<T> mapDataPointToSampleEntry(GFScalarDataPoint<T> point) {
        return new GFSampleEntryJson<>(point.getStart(), point.getEnd(), point.getValue());
    }

    private <T extends Number> GFInstantMeasureSampleEntryJson<T> mapDataPointToInstantMeasureSampleEntry(GFScalarDataPoint<T> point) {
        return new GFInstantMeasureSampleEntryJson<>(point.getStart(),point.getValue());
    }
}


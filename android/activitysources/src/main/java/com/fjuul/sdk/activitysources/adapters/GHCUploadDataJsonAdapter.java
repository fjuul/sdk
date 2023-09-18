package com.fjuul.sdk.activitysources.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.internal.GHCUploadData;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCPowerSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCScalarDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCSessionBundle;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCInstantMeasureSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCIntradayHeartRateSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCIntradaySampleEntryJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCSampleJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCSessionJson;
import com.squareup.moshi.ToJson;

import android.annotation.SuppressLint;

public class GHCUploadDataJsonAdapter {
    @SuppressLint("NewApi")
    @ToJson
    GHCUploadDataJson toJson(GHCUploadData uploadData) {
        final List<GHCSampleJson<GHCIntradaySampleEntryJson<Float>>> caloriesData =
            groupAndMapGHCPointsToJsonSample(uploadData.getCaloriesData(), this::mapDataPointToIntradayEntry);
        final List<GHCSampleJson<GHCIntradaySampleEntryJson<Integer>>> stepsData =
            groupAndMapGHCPointsToJsonSample(uploadData.getStepsData(), this::mapDataPointToIntradayEntry);
        final List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> hrData =
            groupAndMapGHCPointsToJsonSample(uploadData.getHeartRateData(), this::mapDataPointToIntradayHeartRateEntry);
        final List<GHCSessionJson> sessionsData =
            uploadData.getSessionsData().stream().map(this::mapSessionBundleToJson).collect(Collectors.toList());
        return new GHCUploadDataJson(caloriesData, stepsData, hrData, sessionsData);
    }

    @SuppressLint("NewApi")
    private <T extends GHCDataPoint, O> List<GHCSampleJson<O>> groupAndMapGHCPointsToJsonSample(List<T> dataPoints,
        Function<T, O> pointMapper) {
        final Map<Optional<String>, List<T>> groupedByDataSource =
            dataPoints.stream().collect(Collectors.groupingBy(point -> Optional.ofNullable(point.getDataSource())));
        final List<GHCSampleJson<O>> samples = groupedByDataSource.entrySet().stream().map((entry) -> {
            final String dataSource = entry.getKey().orElse(null);
            List<O> entries = entry.getValue().stream().map(pointMapper).collect(Collectors.toList());
            return new GHCSampleJson<>(dataSource, entries);
        }).collect(Collectors.toList());
        return samples;
    }

    private GHCSessionJson mapSessionBundleToJson(GHCSessionBundle sessionBundle) {
        final List<GHCSampleJson<GHCUploadDataJson.GHCSampleEntryJson<Integer>>> activitySegmentsData =
            groupAndMapGHCPointsToJsonSample(sessionBundle.getActivitySegments(), this::mapDataPointToSampleEntry);
        final List<GHCSampleJson<GHCUploadDataJson.GHCSampleEntryJson<Float>>> caloriesData =
            groupAndMapGHCPointsToJsonSample(sessionBundle.getCalories(), this::mapDataPointToSampleEntry);
        final List<GHCSampleJson<GHCUploadDataJson.GHCSampleEntryJson<Integer>>> stepsData =
            groupAndMapGHCPointsToJsonSample(sessionBundle.getSteps(), this::mapDataPointToSampleEntry);
        final List<GHCSampleJson<GHCIntradayHeartRateSampleEntryJson>> hrData =
            groupAndMapGHCPointsToJsonSample(sessionBundle.getHeartRate(), this::mapDataPointToIntradayHeartRateEntry);
        final List<GHCSampleJson<GHCUploadDataJson.GHCIntradayPowerSampleEntryJson>> powerData =
            groupAndMapGHCPointsToJsonSample(sessionBundle.getPower(), this::mapDataPointToIntradayPowerEntry);
        return new GHCSessionJson(sessionBundle.getId(),
            sessionBundle.getTitle(),
            sessionBundle.getNotes(),
            sessionBundle.getTimeStart(),
            sessionBundle.getTimeEnd(),
            sessionBundle.getType(),
            activitySegmentsData,
            caloriesData,
            stepsData,
            hrData,
            powerData);
    }

    private <T extends Number> GHCIntradaySampleEntryJson<T> mapDataPointToIntradayEntry(GHCScalarDataPoint<T> point) {
        return new GHCIntradaySampleEntryJson<>(point.getStart(), point.getValue());
    }

    private GHCIntradayHeartRateSampleEntryJson mapDataPointToIntradayHeartRateEntry(
        GHCHeartRateSummaryDataPoint point) {
        return new GHCIntradayHeartRateSampleEntryJson(point.getStart(),
            point.getAvg(),
            point.getMin(),
            point.getMax());
    }

    private GHCUploadDataJson.GHCIntradayPowerSampleEntryJson mapDataPointToIntradayPowerEntry(
        GHCPowerSummaryDataPoint point) {
        return new GHCUploadDataJson.GHCIntradayPowerSampleEntryJson(point.getStart(),
            point.getAvg(),
            point.getMin(),
            point.getMax());
    }

    private <T extends Number> GHCUploadDataJson.GHCSampleEntryJson<T> mapDataPointToSampleEntry(
        GHCScalarDataPoint<T> point) {
        if (point.getEnd() == null) {
            throw new IllegalStateException("GHCScalarDataPoint must have the defined end time: " + point.toString());
        }
        return new GHCUploadDataJson.GHCSampleEntryJson<>(point.getStart(), point.getEnd(), point.getValue());
    }

    private <T extends Number> GHCInstantMeasureSampleEntryJson<T> mapDataPointToInstantMeasureSampleEntry(
        GHCScalarDataPoint<T> point) {
        return new GHCInstantMeasureSampleEntryJson<>(point.getStart(), point.getValue());
    }
}

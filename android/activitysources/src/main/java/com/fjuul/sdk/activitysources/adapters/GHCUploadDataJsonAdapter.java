package com.fjuul.sdk.activitysources.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.internal.GHCUploadData;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCScalarDataPoint;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCIntradayHeartRateSampleEntryJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCIntradaySampleEntryJson;
import com.fjuul.sdk.activitysources.json.GHCUploadDataJson.GHCSampleJson;
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
        return new GHCUploadDataJson(caloriesData, stepsData, hrData);
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

}

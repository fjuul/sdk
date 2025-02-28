package com.fjuul.sdk.activitysources.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.internal.HCUploadData;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HCScalarDataPoint;
import com.fjuul.sdk.activitysources.json.HCUploadDataJson;
import com.fjuul.sdk.activitysources.json.HCUploadDataJson.HCIntradayHeartRateSampleEntryJson;
import com.fjuul.sdk.activitysources.json.HCUploadDataJson.HCIntradaySampleEntryJson;
import com.fjuul.sdk.activitysources.json.HCUploadDataJson.HCSampleJson;
import com.squareup.moshi.ToJson;

import android.annotation.SuppressLint;

public class HCUploadDataJsonAdapter {
    @SuppressLint("NewApi")
    @ToJson
    HCUploadDataJson toJson(HCUploadData uploadData) {
        final List<HCSampleJson<HCIntradaySampleEntryJson<Float>>> caloriesData =
            groupAndMapHCPointsToJsonSample(uploadData.getCaloriesData(), this::mapDataPointToIntradayEntry);
        final List<HCSampleJson<HCIntradaySampleEntryJson<Integer>>> stepsData =
            groupAndMapHCPointsToJsonSample(uploadData.getStepsData(), this::mapDataPointToIntradayEntry);
        final List<HCSampleJson<HCIntradayHeartRateSampleEntryJson>> hrData =
            groupAndMapHCPointsToJsonSample(uploadData.getHeartRateData(), this::mapDataPointToIntradayHeartRateEntry);
        return new HCUploadDataJson(caloriesData, stepsData, hrData);
    }

    @SuppressLint("NewApi")
    private <T extends HCDataPoint, O> List<HCSampleJson<O>> groupAndMapHCPointsToJsonSample(List<T> dataPoints,
                                                                                             Function<T, O> pointMapper) {
        final Map<Optional<String>, List<T>> groupedByDataSource =
            dataPoints.stream().collect(Collectors.groupingBy(point -> Optional.ofNullable(point.getDataSource())));
        final List<HCSampleJson<O>> samples = groupedByDataSource.entrySet().stream().map((entry) -> {
            final String dataSource = entry.getKey().orElse(null);
            List<O> entries = entry.getValue().stream().map(pointMapper).collect(Collectors.toList());
            return new HCSampleJson<>(dataSource, entries);
        }).collect(Collectors.toList());
        return samples;
    }

    private <T extends Number> HCIntradaySampleEntryJson<T> mapDataPointToIntradayEntry(HCScalarDataPoint<T> point) {
        return new HCIntradaySampleEntryJson<>(point.getStart(), point.getValue());
    }

    private HCIntradayHeartRateSampleEntryJson mapDataPointToIntradayHeartRateEntry(
        HCHeartRateSummaryDataPoint point) {
        return new HCIntradayHeartRateSampleEntryJson(point.getStart(),
            point.getAvg(),
            point.getMin(),
            point.getMax());
    }

}

package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GFDataUtils {
    @SuppressLint("NewApi")
    public <V, T extends GFDataPoint<V>> List<GFDataPointsBatch<T>> groupPointsIntoBatchesByDuration(Date start, Date end, List<T> points, Duration duration) {
        Date leftBorderRange = start;
        Date rightBorderRange = Date.from(start.toInstant().plusMillis(duration.toMillis()));
        List<GFDataPointsBatch<T>> batches = new ArrayList<>();
        while (leftBorderRange.before(end)) {
            Date finalLeftBorderRange = leftBorderRange;
            Date finalRightBorderRange = rightBorderRange;
            // TODO: use immutable list for grouped points
            List<T> groupedPoints = points.stream().filter((dataPoint) -> {
                return dataPoint.start.compareTo(finalLeftBorderRange) >= 0 && dataPoint.start.compareTo(finalRightBorderRange) == -1;
            }).collect(Collectors.toList());
            batches.add(new GFDataPointsBatch(groupedPoints, leftBorderRange, rightBorderRange));
            leftBorderRange = rightBorderRange;
            rightBorderRange = Date.from(rightBorderRange.toInstant().plusMillis(duration.toMillis()));
        }
        return batches;
    }
}

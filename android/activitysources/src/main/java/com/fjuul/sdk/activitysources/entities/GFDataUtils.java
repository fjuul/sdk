package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.core.util.Pair;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class GFDataUtils {
    private ZoneId zoneId;
    private Clock clock;

    @SuppressLint("NewApi")
    public GFDataUtils() {
        this(ZoneId.systemDefault(), Clock.systemUTC());
    }

    GFDataUtils(ZoneId zoneId, Clock clock) {
        this.zoneId = zoneId;
        this.clock = clock;
    }

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

    @SuppressLint("NewApi")
    public Pair<Date, Date> adjustInputDatesForGFRequest(LocalDate start, LocalDate end) {
        final Date startDate = Date.from(start.atStartOfDay(zoneId).toInstant());
        final Date endOfDayOfEndDate = Date.from(end.atTime(LocalTime.MAX).atZone(zoneId).toInstant());
        final Date currentMoment = Date.from(clock.instant());

        Date endDate = Collections.min(Arrays.asList(endOfDayOfEndDate, currentMoment));
        return new Pair(startDate, endDate);
    }

    @SuppressLint("NewApi")
    public Pair<Date, Date> adjustInputDatesForBatches(LocalDate start, LocalDate end, Duration batchDuration) {
        final Date startDate = Date.from(start.atStartOfDay(zoneId).toInstant());
        Date roundedCurrentDate;
        final Instant currentInstant = Instant.now(clock);
        Instant roundedInstant;
        final Instant zonedInstant = ZonedDateTime.ofInstant(currentInstant, zoneId).toInstant();
        long millisSpentInBatch = zonedInstant.toEpochMilli() % batchDuration.toMillis();

        if (millisSpentInBatch == 0) {
            roundedInstant = zonedInstant;
        } else {
            roundedInstant = zonedInstant.minusMillis(millisSpentInBatch).plusMillis(batchDuration.toMillis());
        }
        roundedCurrentDate = Date.from(roundedInstant);
        final Date nextDayAfterEnd = Date.from(end.plusDays(1).atStartOfDay(zoneId).toInstant());
        final Date endDate = Collections.min(Arrays.asList(nextDayAfterEnd, roundedCurrentDate));
        return new Pair(startDate, endDate);
    }

    @SuppressLint("NewApi")
    public List<Pair<Date, Date>> splitDateRangeIntoChunks(Date start, Date end, Duration duration) {
        if (start.equals(end)) {
            return Arrays.asList(new Pair<>(start, end));
        }
        Date leftBorder = start;
        List<Pair<Date, Date>> dateRanges = new ArrayList<>();
        while (leftBorder.before(end)) {
            final Date nextStep = Date.from(leftBorder.toInstant().plusMillis(duration.toMillis()));
            final Date rightBorder = Collections.min(Arrays.asList(nextStep, end));
            dateRanges.add(new Pair<>(leftBorder, rightBorder));
            leftBorder = rightBorder;
        }
        return dateRanges;
    }
}

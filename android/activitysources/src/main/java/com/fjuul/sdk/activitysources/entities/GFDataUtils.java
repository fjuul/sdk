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

/**
 * Utility class for processing and querying google fit data.
 */
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

    /**
     * Generates a list of batches with the duration for the specified time interval containing GF data points according to the start time of a data point.
     * A batch will be created even if there are not any points matched with the time interval of the batch.
     * @param start start time since the first batch will be created.
     * @param end end time of the batching. If this parameter doesn't satisfy the strict batch division then the end time of the last batch will be rounded by the duration.
     * @param points data points that need to group into batches
     * @param duration duration of a batch
     * @return list of batches
     */
    @SuppressLint("NewApi")
    public <T extends GFDataPoint> List<GFDataPointsBatch<T>> groupPointsIntoBatchesByDuration(Date start, Date end, List<T> points, Duration duration) {
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

    /**
     * Transforms dates of a request to a pair of dates in the local timezone.
     * The input start date will be considered as the start of a day, and the input end date as the end of a day or the current moment if it belongs to today.
     * @param start local start date
     * @param end local end date
     * @return pair of adjusted input dates
     */
    @SuppressLint("NewApi")
    public Pair<Date, Date> adjustInputDatesForGFRequest(LocalDate start, LocalDate end) {
        final Date startDate = Date.from(start.atStartOfDay(zoneId).toInstant());
        final Date endOfDayOfEndDate = Date.from(end.atTime(LocalTime.MAX).atZone(zoneId).toInstant());
        final Date currentMoment = Date.from(clock.instant());

        Date endDate = Collections.min(Arrays.asList(endOfDayOfEndDate, currentMoment));
        return new Pair(startDate, endDate);
    }


    /**
     * Transforms dates of a request to a pair of dates for the batching in the local timezone.
     * The input start date will be considered as the start of a day.
     * The input end date will be considered as the end of a day or the current moment rounded up by
     * the duration if it belongs to today (for example, the current moment at 10:45 will be rounded up 11:00 for 30 minutes duration).
     * @param start local start date
     * @param end local start date
     * @param batchDuration duration of batches
     * @return pair of adjusted input dates
     */
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

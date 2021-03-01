package com.fjuul.sdk.activitysources.entities.internal;

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

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

/**
 * Utility class for processing and querying google fit data.
 */
public class GFDataUtils {
    @NonNull
    private ZoneId zoneId;
    @NonNull
    private Clock clock;

    @SuppressLint("NewApi")
    public GFDataUtils() {
        this(ZoneId.systemDefault(), Clock.systemUTC());
    }

    GFDataUtils(@NonNull ZoneId zoneId, @NonNull Clock clock) {
        this.zoneId = zoneId;
        this.clock = clock;
    }

    /**
     * Generates a list of batches with the duration for the specified time interval containing GF data points according
     * to the start time of a data point. A batch will be created even if there are not any points matched with the time
     * interval of the batch.
     *
     * @param start start time since the first batch will be created.
     * @param end end time of the batching. If this parameter doesn't satisfy the strict batch division then the end
     *        time of the last batch will be rounded by the duration.
     * @param points data points that need to group into batches
     * @param duration duration of a batch
     * @return list of batches
     */
    @SuppressLint("NewApi")
    @NonNull
    public <T extends GFDataPoint> List<GFDataPointsBatch<T>> groupPointsIntoBatchesByDuration(@NonNull Date start,
        @NonNull Date end,
        @NonNull List<T> points,
        @NonNull Duration duration) {
        Date leftBorderRange = start;
        Date rightBorderRange = Date.from(start.toInstant().plusMillis(duration.toMillis()));
        final List<GFDataPointsBatch<T>> batches = new ArrayList<>();
        while (leftBorderRange.before(end)) {
            final Date finalLeftBorderRange = leftBorderRange;
            final Date finalRightBorderRange = rightBorderRange;
            final List<T> groupedPoints = points.stream().filter((dataPoint) -> {
                return dataPoint.getStart().compareTo(finalLeftBorderRange) >= 0
                    && dataPoint.getStart().compareTo(finalRightBorderRange) == -1;
            }).collect(Collectors.toList());
            batches.add(new GFDataPointsBatch(groupedPoints, leftBorderRange, rightBorderRange));
            leftBorderRange = rightBorderRange;
            rightBorderRange = Date.from(rightBorderRange.toInstant().plusMillis(duration.toMillis()));
        }
        return batches;
    }

    /**
     * Transforms dates of a request to a pair of dates in the local timezone. The input start date will be considered
     * as the start of a day, and the input end date as the end of a day or the current moment if it belongs to today.
     *
     * @param start local start date
     * @param end local end date
     * @return pair of adjusted input dates
     */
    @SuppressLint("NewApi")
    @NonNull
    public Pair<Date, Date> adjustInputDatesForGFRequest(@NonNull LocalDate start, @NonNull LocalDate end) {
        final Date startDate = Date.from(start.atStartOfDay(zoneId).toInstant());
        final Date endOfDayOfEndDate = Date.from(end.atTime(LocalTime.MAX).atZone(zoneId).toInstant());
        final Date currentMoment = Date.from(clock.instant());

        Date endDate = Collections.min(Arrays.asList(endOfDayOfEndDate, currentMoment));
        return new Pair(startDate, endDate);
    }


    /**
     * Transforms dates of a request to a pair of dates for the batching in the local timezone. The input start date
     * will be considered as the start of a day. The input end date will be considered as the end of a day or the
     * current moment rounded up by the duration if it belongs to today (for example, the current moment at 10:45 will
     * be rounded up 11:00 for 30 minutes duration).
     *
     * @param start local start date
     * @param end local start date
     * @param batchDuration duration of batches
     * @return pair of adjusted input dates
     */
    @SuppressLint("NewApi")
    @NonNull
    public Pair<Date, Date> adjustInputDatesForBatches(@NonNull LocalDate start,
        @NonNull LocalDate end,
        @NonNull Duration batchDuration) {
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
    @NonNull
    public Pair<Date, Date> roundDatesByIntradayBatchDuration(@NonNull Date start,
        @NonNull Date end,
        @NonNull Duration batchDuration) {
        if (Duration.ofDays(1).toMillis() % batchDuration.toMillis() != 0) {
            throw new IllegalArgumentException(
                "The batch duration must fit into the duration of the day without any remainder");
        }
        final Date beginningOfStartDate =
            Date.from(start.toInstant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
        final long intradayMillisSpentInStartDate = start.getTime() - beginningOfStartDate.getTime();
        Date roundedStartDate;
        final long startDateRemainderMillis = intradayMillisSpentInStartDate % batchDuration.toMillis();
        if (startDateRemainderMillis == 0) {
            roundedStartDate = start;
        } else {
            roundedStartDate = Date.from(start.toInstant().minusMillis(startDateRemainderMillis));
        }

        final Date beginningOfEndDate =
            Date.from(end.toInstant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
        final long intradayMillisSpentInEndDate = end.getTime() - beginningOfEndDate.getTime();
        Date roundedEndDate;
        final long endDateRemainderMillis = intradayMillisSpentInEndDate % batchDuration.toMillis();
        if (endDateRemainderMillis == 0) {
            roundedEndDate = end;
        } else {
            roundedEndDate =
                Date.from(end.toInstant().minusMillis(endDateRemainderMillis).plusMillis(batchDuration.toMillis()));
        }
        return new Pair(roundedStartDate, roundedEndDate);
    }

    @SuppressLint("NewApi")
    @NonNull
    public List<Pair<Date, Date>> splitDateRangeIntoChunks(@NonNull Date start,
        @NonNull Date end,
        @NonNull Duration duration) {
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

    @SuppressLint("NewApi")
    public List<Pair<Date, Date>> splitDateRangeIntoDays(@NonNull Date start, @NonNull Date end) {
        if (start.equals(end)) {
            return Arrays.asList(new Pair<>(start, end));
        }
        final List<Pair<Date, Date>> dateRanges = new ArrayList<>();
        Date iterDate = start;
        while (iterDate.before(end)) {
            final Date startOfDay = Date.from(iterDate.toInstant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant());
            final Date endOfDay = Date.from(iterDate.toInstant().atZone(zoneId).toLocalDate().atTime(LocalTime.MAX).atZone(zoneId).toInstant());
            final Date leftBorder = Collections.max(Arrays.asList(iterDate, startOfDay));
            final Date rightBorder = Collections.min(Arrays.asList(end, endOfDay));
            dateRanges.add(new Pair<>(leftBorder, rightBorder));
            iterDate = Date.from(iterDate.toInstant().atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant());
        }
        return dateRanges;
    }
}

package com.fjuul.sdk.activitysources.entities.internal;

import android.os.Build;

import androidx.core.util.Pair;


import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.GFDataUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GFDataUtilsTest {
    public static final String dataSourceId = "derived:com.google.calories.expended:com.google.android.gms:from_activities";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static class GroupCaloriesIntoBatchesByDurationTest extends GivenRobolectricContext {
        GFDataUtils gfDataUtils;
        List<GFCalorieDataPoint> calories = Stream.of(
            new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-01-01T10:05:00Z")), dataSourceId),
            new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-01-01T10:07:00Z")), dataSourceId),
            new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-01-01T10:31:00Z")), dataSourceId),
            new GFCalorieDataPoint(2.5421f, Date.from(Instant.parse("2020-01-01T10:34:00Z")), dataSourceId)
        ).collect(Collectors.toList());

        @Test
        public void groupPointsIntoBatchesByDuration_bigDurationCoversAllPoints_returnsOneBatchWithAllPoints() {
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T11:00:00Z"));
            Duration duration = Duration.ofHours(1);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should have only one batch",
                1,
                batches.size());
            assertEquals("batch should have all calories", calories.size(), batches.get(0).getPoints().size());
        }

        @Test
        public void groupPointsIntoBatchesByDuration_smallerDurationSplitsPoints_returnsTwoBatchesWithPoints() {
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T11:00:00Z"));
            Duration duration = Duration.ofMinutes(30);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should have 2 batches",
                2,
                batches.size());
            assertEquals("first batch has 2 points",
                2,
                batches.get(0).getPoints().size());
            assertThat("first batch has two first points",
                calories.subList(0, 2),
                equalTo(batches.get(0).getPoints()));
            assertEquals("second batch has 2 points",
                2,
                batches.get(1).getPoints().size());
            assertThat("second batch has 2 last points",
                calories.subList(2, 4),
                equalTo(batches.get(1).getPoints()));
        }

        @Test
        public void groupPointsIntoBatchesByDuration_smallerDurationWithoutMatches_returnsBatchesWithBlank() {
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T11:00:00Z"));
            Duration duration = Duration.ofMinutes(20);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should have 3 batches",
                3,
                batches.size());
            assertEquals("first batch has 2 points",
                2,
                batches.get(0).getPoints().size());
            assertThat("first batch has two first points",
                calories.subList(0, 2),
                equalTo(batches.get(0).getPoints()));
            assertEquals("second batch has 2 points",
                2,
                batches.get(1).getPoints().size());
            assertThat("second batch has two last points",
                calories.subList(2, 4),
                equalTo(batches.get(1).getPoints()));
            assertEquals("third batch has no points",
                0,
                batches.get(2).getPoints().size());
        }

        @Test
        public void groupPointsIntoBatchesByDuration_leftBorderIsInclusiveAndRightBorderIsExclusive_returnsBatches() {
            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-01-01T10:05:00Z")), dataSourceId),
                new GFCalorieDataPoint(2.5421f, Date.from(Instant.parse("2020-01-01T10:30:00Z")), dataSourceId)
            ).collect(Collectors.toList());
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T11:00:00Z"));
            Duration duration = Duration.ofMinutes(30);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should have 2 batches",
                2,
                batches.size());
            assertEquals("first batch has 1 point",
                1,
                batches.get(0).getPoints().size());
            assertThat("first batch has the first point",
                calories.subList(0, 1),
                equalTo(batches.get(0).getPoints()));
            assertEquals("second batch has 1 point",
                1,
                batches.get(1).getPoints().size());
            assertThat("second batch has the last point",
                calories.subList(1, 2),
                equalTo(batches.get(1).getPoints()));
        }

        @Test
        public void groupPointsIntoBatchesByDuration_durationСrossesEndTime_returnsBatchWithEndTime() {
            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-01-01T10:05:00Z")), dataSourceId),
                new GFCalorieDataPoint(2.5421f, Date.from(Instant.parse("2020-01-01T10:30:00Z")), dataSourceId)
            ).collect(Collectors.toList());
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T10:45:00Z"));
            Duration duration = Duration.ofMinutes(30);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should have 2 batches",
                2,
                batches.size());
            assertEquals("first batch has 1 point",
                1,
                batches.get(0).getPoints().size());
            assertThat("first batch has the first point",
                calories.subList(0, 1),
                equalTo(batches.get(0).getPoints()));
            assertEquals("second batch has 1 point",
                1,
                batches.get(1).getPoints().size());
            assertThat("second batch has the last point",
                calories.subList(1, 2),
                equalTo(batches.get(1).getPoints()));
            assertThat("the last batch should have end time according to the duration",
                batches.get(1).getEndTime(),
                equalTo(Date.from(Instant.parse("2020-01-01T11:00:00Z"))));
        }

        @Test
        public void groupPointsIntoBatchesByDuration_singleDurationСrossesEndTime_returnsOneBatchWithCorrectEndTimeOfDuration() {
            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-01-01T10:05:00Z")), dataSourceId),
                new GFCalorieDataPoint(2.5421f, Date.from(Instant.parse("2020-01-01T10:30:00Z")), dataSourceId)
            ).collect(Collectors.toList());
            Date start = Date.from(Instant.parse("2020-01-01T10:00:00Z"));
            Date end = Date.from(Instant.parse("2020-01-01T10:45:00Z"));
            Duration duration = Duration.ofHours(1);
            GFDataUtils gfDataUtils = new GFDataUtils();
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = gfDataUtils.groupPointsIntoBatchesByDuration(start, end, calories, duration);
            assertEquals("should return single batch",
                1,
                batches.size());
            assertThat("batch has all points",
                calories,
                equalTo(batches.get(0).getPoints()));
            assertThat("batch should have correct end time (start time + duration)",
                Date.from(Instant.parse("2020-01-01T11:00:00Z")),
                equalTo(batches.get(0).getEndTime()));
        }
    }

    public static class AdjustInputDatesForGFRequest extends GivenRobolectricContext {
        Clock fixedClock;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
        }

        @Test
        public void adjustInputDatesForGFRequest_whenStartAndEndArePastTime_returnsStartOfDayOfStartAndEndOfDayOfEnd() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-01");
            LocalDate end = LocalDate.parse("2020-09-05");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForGFRequest(start, end);
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-08-31T14:00:00Z"))));
            assertThat("end should be a end of the day of endDate date",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-05T13:59:59Z").plusMillis(999))));
        }

        @Test
        public void adjustInputDatesForGFRequest_whenStartIsPastAndEndIsToday_returnsStartOfDayOfStartAndEndTimeNearToCurrentMoment() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-01");
            LocalDate end = LocalDate.parse("2020-09-16");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForGFRequest(start, end);
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-08-31T14:00:00Z"))));
            assertThat("end should be the current moment",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-15T21:30:00Z"))));
        }

        @Test
        public void adjustInputDatesForGFRequest_whenStartAndEndIsTheSameDayInThePast_returnsStartAndEndOfTheDay() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-10");
            LocalDate end = LocalDate.parse("2020-09-10");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForGFRequest(start, end);
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-09-09T14:00:00Z"))));
            assertThat("end should be an end of the end date",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-10T13:59:59Z").plusMillis(999))));
        }

        @Test
        public void adjustInputDatesForGFRequest_whenStartAndEndIsToday_returnsStartOfTodayAndCurrentMoment() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-16");
            LocalDate end = LocalDate.parse("2020-09-16");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForGFRequest(start, end);
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-09-15T14:00:00Z"))));
            assertThat("end should be the current moment",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-15T21:30:00Z"))));
        }
    }

    public static class AdjustInputDatesForBatches extends GivenRobolectricContext {
        Clock fixedClock;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:17:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
        }

        @Test
        public void adjustInputDatesForBatches_whenStartAndEndArePastTime_returnsStartOfDayOfStartAndStartOfDayFollowingEndDate() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-01");
            LocalDate end = LocalDate.parse("2020-09-05");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForBatches(start, end, Duration.ofMinutes(30));
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-08-31T14:00:00Z"))));
            assertThat("end should be a start of the day following end date",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-05T14:00:00Z"))));
        }

        @Test
        public void adjustInputDatesForBatches_whenStartIsPastAndEndIsToday_returnsStartOfDayOfStartAndEndTimeRoundedByDuration() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-01");
            LocalDate end = LocalDate.parse("2020-09-16");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForBatches(start, end, Duration.ofMinutes(30));
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-08-31T14:00:00Z"))));
            assertThat("end should be rounded to the current moment by duration",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-15T21:30:00Z"))));
        }

        @Test
        public void adjustInputDatesForBatches_whenStartAndEndIsTheSameDayInThePast_returnsStartOfTheDayAndStartOfTheNextDay() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-10");
            LocalDate end = LocalDate.parse("2020-09-10");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForBatches(start, end, Duration.ofMinutes(30));
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-09-09T14:00:00Z"))));
            assertThat("end should be a start of a day after the end date",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-10T14:00:00Z"))));
        }

        @Test
        public void adjustInputDatesForBatches_whenStartAndEndIsToday_returnsStartOfTheDayAndCurrentMomentRoundedByDuration() {
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-16");
            LocalDate end = LocalDate.parse("2020-09-16");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForBatches(start, end, Duration.ofMinutes(30));
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-09-15T14:00:00Z"))));
            assertThat("end should be rounded to the current moment by duration",
                pair.second,
                equalTo(Date.from(Instant.parse("2020-09-15T21:30:00Z"))));
        }

        @Test
        public void adjustInputDatesForBatches_whenStartAndEndIsTodayAndCurrentInstantIsBorder_returnsEndWithInitialValue() {
            String instantExpected = "2020-09-15T20:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneOffset.UTC);
            GFDataUtils gfDataUtils = new GFDataUtils(ZoneId.of("Australia/Sydney"), fixedClock);
            LocalDate start = LocalDate.parse("2020-09-16");
            LocalDate end = LocalDate.parse("2020-09-16");
            Pair<Date, Date> pair = gfDataUtils.adjustInputDatesForBatches(start, end, Duration.ofMinutes(30));
            assertThat("start should be a start of the day of start date",
                pair.first,
                equalTo(Date.from(Instant.parse("2020-09-15T14:00:00Z"))));
            assertThat("end should equal to the current moment without rounding",
                pair.second,
                equalTo(Date.from(Instant.parse(instantExpected))));
        }
    }

    public static class SplitDateRangeIntoChunksTest extends GivenRobolectricContext {
        @Test
        public void splitDateRangeIntoChunks_whenStartAndEndIsTheSameTime_returnsPairOfTheDate() {
            GFDataUtils gfDataUtils = new GFDataUtils();
            final Date date = Date.from(Instant.parse("2020-08-31T14:00:00Z"));
            final Duration duration = Duration.ofHours(12);
            List<Pair<Date, Date>> chunks = gfDataUtils.splitDateRangeIntoChunks(date, date, duration);
            assertEquals("should return only one pair",
                1,
                chunks.size());
            assertThat("first entry should be the time",
                chunks.get(0).first,
                equalTo(date)
            );
            assertThat("second entry should be the time",
                chunks.get(0).second,
                equalTo(date)
            );
        }

        @Test
        public void splitDateRangeIntoChunks_whenEndIsLessThanStartPlusDuration_returnsPairOfStartAndEnd() {
            GFDataUtils gfDataUtils = new GFDataUtils();
            final Date start = Date.from(Instant.parse("2020-08-31T14:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-08-31T17:00:00Z"));
            final Duration duration = Duration.ofHours(12);
            List<Pair<Date, Date>> chunks = gfDataUtils.splitDateRangeIntoChunks(start, end, duration);
            assertEquals("should return only one pair",
                1,
                chunks.size());
            assertThat("first entry should be the start",
                chunks.get(0).first,
                equalTo(start)
            );
            assertThat("second entry should be the end",
                chunks.get(0).second,
                equalTo(end)
            );
        }

        @Test
        public void splitDateRangeIntoChunks_whenEndCoversStartPlusDuration_returnsPairOfStartAndEnd() {
            GFDataUtils gfDataUtils = new GFDataUtils();
            final Date start = Date.from(Instant.parse("2020-08-31T14:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-09-01T02:00:00Z"));
            final Duration duration = Duration.ofHours(12);
            List<Pair<Date, Date>> chunks = gfDataUtils.splitDateRangeIntoChunks(start, end, duration);
            assertEquals("should return only one pair",
                1,
                chunks.size());
            assertThat("first entry should be the start",
                chunks.get(0).first,
                equalTo(start)
            );
            assertThat("second entry should be the end",
                chunks.get(0).second,
                equalTo(end)
            );
        }

        @Test
        public void splitDateRangeIntoChunks_whenEndIsMoreThanStartPlusDuration_returnsPairs() {
            GFDataUtils gfDataUtils = new GFDataUtils();
            final Date start = Date.from(Instant.parse("2020-08-31T14:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-09-01T09:00:00Z"));
            final Duration duration = Duration.ofHours(12);
            List<Pair<Date, Date>> chunks = gfDataUtils.splitDateRangeIntoChunks(start, end, duration);
            assertEquals("should return 2 pairs",
                2,
                chunks.size());
            assertThat("first entry should be the start",
                chunks.get(0).first,
                equalTo(start)
            );
            assertThat("second entry should be the start + duration",
                chunks.get(0).second,
                equalTo(Date.from(Instant.parse("2020-09-01T02:00:00Z")))
            );

            assertThat("first entry of the next pair should be the start + duration",
                chunks.get(1).first,
                equalTo(Date.from(Instant.parse("2020-09-01T02:00:00Z")))
            );
            assertThat("second entry of the next pair should be the end",
                chunks.get(1).second,
                equalTo(end)
            );
        }
    }

}

package com.fjuul.sdk.activitysources.entities;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GFDataUtilsTest {
    public static final String dataSourceId = "derived:com.google.calories.expended:com.google.android.gms:from_activities";
    public static class GroupCaloriesIntoBatchesByDurationTest {
        GFDataUtils gfDataUtils;
        List<GFCalorieDataPoint> calories = Stream.of(
            new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-01-01T10:05:00Z")), dataSourceId),
            new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-01-01T10:07:00Z")), dataSourceId),
            new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-01-01T10:31:00Z")), dataSourceId),
            new GFCalorieDataPoint(2.5421f, Date.from(Instant.parse("2020-01-01T10:34:00Z")), dataSourceId)
        ).collect(Collectors.toList());

        @Before
        public void beforeTests() {
        }

        @Test
        public void groupPointsIntoBatchesByDuration_bigDurationCoversAllPoints_returnOneBatchWithAllPoints() {
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
        public void groupPointsIntoBatchesByDuration_smallerDurationSplitsPoints_returnTwoBatchesWithPoints() {
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
        public void groupPointsIntoBatchesByDuration_smallerDurationWithoutMatches_returnBatchesWithBlank() {
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
        public void groupPointsIntoBatchesByDuration_leftBorderIsInclusiveAndRightBorderIsExclusive_returnBatches() {
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
        public void groupPointsIntoBatchesByDuration_durationСrossesEndTime_returnBatchWithEndTime() {
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
        public void groupPointsIntoBatchesByDuration_singleDurationСrossesEndTime_returnOneBatchWithCorrectEndTimeOfDuration() {
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

        // case 3: when endDate is smaller than startDate + duration
    }

}
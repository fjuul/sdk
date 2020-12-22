package com.fjuul.sdk.activitysources.entities;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class GFIntradaySyncOptionsTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static class BuilderTest extends GivenRobolectricContext {
        static final String fixedInstant = "2020-12-01T15:56:23Z";
        Clock testClock;

        @Before
        public void beforeTest() {
            testClock = Clock.fixed(Instant.parse(fixedInstant), ZoneId.of("UTC"));
        }

        @Test
        public void setDateRange_whenStartDateIsAfterEndDate_throwsException() {
            final GFIntradaySyncOptions.Builder subject = new GFIntradaySyncOptions.Builder(testClock);
            try {
                subject.setDateRange(LocalDate.parse("2020-12-05"), LocalDate.parse("2020-12-01"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "The start date must be less or equal to the end date",
                    exc.getMessage());
            }
        }

        @Test
        public void setDateRange_whenEndDatePointsAtTheFuture_throwsException() {
            final GFIntradaySyncOptions.Builder subject = new GFIntradaySyncOptions.Builder(testClock);
            try {
                subject.setDateRange(LocalDate.parse("2020-11-27"), LocalDate.parse("2020-12-05"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "The end date must not point at the future",
                    exc.getMessage());
            }
        }

        @Test
        public void setDateRange_whenStartDateExceedsLimit_throwsException() {
            Clock clock1 = Clock.fixed(Instant.parse("2020-02-29T15:56:23Z"), ZoneId.of("UTC"));
            GFIntradaySyncOptions.Builder subject = new GFIntradaySyncOptions.Builder(clock1);

            try {
                subject.setDateRange(LocalDate.parse("2020-01-29"), LocalDate.parse("2020-02-29"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "Input dates must not exceed the max allowed border",
                    exc.getMessage());
            }

            Clock clock2 = Clock.fixed(Instant.parse("2020-02-28T15:56:23Z"), ZoneId.of("UTC"));
            subject = new GFIntradaySyncOptions.Builder(clock2);
            try {
                subject.setDateRange(LocalDate.parse("2020-01-28"), LocalDate.parse("2020-02-28"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "Input dates must not exceed the max allowed border",
                    exc.getMessage());
            }

            Clock clock3 = Clock.fixed(Instant.parse("2020-02-15T15:56:23Z"), ZoneId.of("UTC"));
            subject = new GFIntradaySyncOptions.Builder(clock3);
            try {
                subject.setDateRange(LocalDate.parse("2020-01-15"), LocalDate.parse("2020-02-15"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "Input dates must not exceed the max allowed border",
                    exc.getMessage());
            }

            Clock clock4 = Clock.fixed(Instant.parse("2020-08-31T15:56:23Z"), ZoneId.of("UTC"));
            subject = new GFIntradaySyncOptions.Builder(clock4);
            try {
                subject.setDateRange(LocalDate.parse("2020-07-31"), LocalDate.parse("2020-08-31"));
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalArgumentException.class));
                assertEquals("should have message",
                    "Input dates must not exceed the max allowed border",
                    exc.getMessage());
            }
        }

        @Test
        public void setDateRange_whenDateRangeIsValid_doNotThrowException() {
            final GFIntradaySyncOptions.Builder subject = new GFIntradaySyncOptions.Builder(testClock);
            try {
                subject.setDateRange(LocalDate.parse("2020-11-02"), LocalDate.parse("2020-12-01"));
                assertTrue("shouldn't throw the exception", true);
            } catch (Exception exc) {
                fail();
            }
        }
    }
}

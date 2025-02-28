package com.fjuul.sdk.activitysources.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Build;

@RunWith(Enclosed.class)
public class TrackerValueTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    abstract static class GivenRobolectricContext {}

    @RunWith(Enclosed.class)
    public static class StaticMethods {
        public static class ValuesTests extends GivenRobolectricContext {
            @Test
            public void values_whenEverythingIsOK_returnsListOfSupportedTrackerValueByThisVersion() {
                assertThat(TrackerValue.values(),
                    containsInAnyOrder(TrackerValue.FITBIT,
                        TrackerValue.POLAR,
                        TrackerValue.GARMIN,
                        TrackerValue.OURA,
                        TrackerValue.SUUNTO,
                        TrackerValue.WITHINGS,
                        TrackerValue.GOOGLE_FIT));
            }
        }

        public static class ForValueTests extends GivenRobolectricContext {
            @Test
            public void forValue_whenUnknownValue_returnsNull() {
                assertNull(TrackerValue.forValue("healthkit"));
            }

            @Test
            public void forValue_whenKnownValue_returnsAppropriateTrackerValue() {
                assertEquals(TrackerValue.GARMIN, TrackerValue.forValue("garmin"));
            }
        }
    }
}

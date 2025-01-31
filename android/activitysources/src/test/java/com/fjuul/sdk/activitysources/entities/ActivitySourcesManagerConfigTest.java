package com.fjuul.sdk.activitysources.entities;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Build;

@RunWith(Enclosed.class)
public class ActivitySourcesManagerConfigTest {

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static class BuilderTests extends GivenRobolectricContext {
        @Test
        public void build_whenProfileBackgroundSyncModeIsNotSet_throwsException() {
            final ActivitySourcesManagerConfig.Builder subject = new ActivitySourcesManagerConfig.Builder();
            subject
                .setCollectableFitnessMetrics(
                    Stream
                        .of(FitnessMetricsType.INTRADAY_STEPS,
                            FitnessMetricsType.INTRADAY_CALORIES,
                            FitnessMetricsType.INTRADAY_HEART_RATE)
                        .collect(Collectors.toSet()))
                .enableGoogleFitBackgroundSync(Duration.ofMinutes(60));
            try {
                subject.build();
                assertTrue("should throw the exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(NullPointerException.class));
                assertEquals("should have message",
                    "GoogleFit profile background sync mode must be set",
                    exc.getMessage());
            }
        }
    }
}

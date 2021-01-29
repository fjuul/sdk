package com.fjuul.sdk.activitysources.entities.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.ActivitySource;
import com.fjuul.sdk.activitysources.entities.FitbitActivitySource;
import com.fjuul.sdk.activitysources.entities.GarminActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.PolarActivitySource;
import com.fjuul.sdk.activitysources.entities.SuuntoActivitySource;
import com.fjuul.sdk.activitysources.entities.UnknownActivitySource;

import android.os.Build;

@RunWith(Enclosed.class)
public class ActivitySourceResolverTest {

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    abstract static class GivenRobolectricContext {}

    public static class GetInstanceByTrackerValueTest extends GivenRobolectricContext {
        GoogleFitActivitySource mockedGF;
        ActivitySourceResolver subject;

        @Before
        public void beforeTest() {
            mockedGF = mock(GoogleFitActivitySource.class);
            subject = new ActivitySourceResolver();
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsPolar_returnsPolarActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("polar");
                assertThat(result, instanceOf(PolarActivitySource.class));
            }
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsGarmin_returnsGarminActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("garmin");
                assertThat(result, instanceOf(GarminActivitySource.class));
            }
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsSuunto_returnsSuuntoActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("suunto");
                assertThat(result, instanceOf(SuuntoActivitySource.class));
            }
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsFitbit_returnsFitbitActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("fitbit");
                assertThat(result, instanceOf(FitbitActivitySource.class));
            }
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsGooglefit_returnsGooglefitActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("googlefit");
                assertThat(result, instanceOf(GoogleFitActivitySource.class));
                assertEquals(mockedGF, result);
            }
        }

        @Test
        public void getInstanceByTrackerValue_whenInputValueIsUnknown_returnsUnknownActivitySource() {
            try (final MockedStatic<GoogleFitActivitySource> staticMockGF = mockStatic(GoogleFitActivitySource.class)) {
                staticMockGF.when(() -> GoogleFitActivitySource.getInstance()).thenReturn(mockedGF);
                final ActivitySource result = subject.getInstanceByTrackerValue("healthkit");
                assertThat(result, instanceOf(UnknownActivitySource.class));
            }
        }
    }
}

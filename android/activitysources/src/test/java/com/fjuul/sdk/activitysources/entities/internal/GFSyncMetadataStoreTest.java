package com.fjuul.sdk.activitysources.entities.internal;

import android.os.Build;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFActivitySegmentDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFPowerDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSpeedDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.core.entities.IStorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GFSyncMetadataStoreTest {
    final static String USER_TOKEN = "USER_TOKEN";
    public static final String CALORIES_DATA_SOURCE = "derived:com.google.calories.expended:com.google.android.gms:from_activities";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static class SaveSyncMetadataOfCaloriesTest extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void saveSyncMetadataOfCalories_createsAndSavesSyncMetadataInUTC() {
            GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            gfSyncMetadataStore.saveSyncMetadataOfCalories(caloriesBatch);
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            final String expectedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            verify(mockedStorage, times(1)).set(expectedKey, expectedJson);
        }
    }

    public static class SaveSyncMetadataOfHRTest extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void saveSyncMetadataOfHR_createsAndSavesSyncMetadataInUTC() {
            GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            gfSyncMetadataStore.saveSyncMetadataOfHR(hrBatch);
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10T10:00-D10T11:00";
            final String expectedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"sumOfAverages\":212.41478}";
            verify(mockedStorage, times(1)).set(expectedKey, expectedJson);
        }
    }

    public static class SaveSyncMetadataOfStepsTest extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void saveSyncMetadataOfSteps_createsAndSavesSyncMetadataInUTC() {
            GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            gfSyncMetadataStore.saveSyncMetadataOfSteps(stepsBatch);
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            final String expectedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalSteps\":10064}";
            verify(mockedStorage, times(1)).set(expectedKey, expectedJson);
        }
    }

    public static class SaveSyncMetadataOfSessionBundleTest extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void saveSyncMetadataOfSession_createsAndSavesSyncMetadataInUTC() {
            final GFSessionBundle session = new GFSessionBundle.Builder().setId("679acf3c-3d38-4931-8822-0b355d8134e1")
                .setName("short walk")
                .setApplicationIdentifier("com.google.android.apps.fitness")
                .setTimeStart(Date.from(Instant.parse("2020-10-16T11:20:00.000Z")))
                .setTimeEnd(Date.from(Instant.parse("2020-10-16T13:00:00.000Z")))
                .setActivityType("walking")
                .setType(7)
                .setActivitySegments(TestSessionSamplesData.activitySegments)
                .setCalories(TestSessionSamplesData.calories)
                .setHeartRate(TestSessionSamplesData.heartRate)
                .setPower(TestSessionSamplesData.power)
                .setSpeed(TestSessionSamplesData.speed)
                .setSteps(TestSessionSamplesData.steps)
                .build();
            gfSyncMetadataStore.saveSyncMetadataOfSession(session);
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.session.679acf3c-3d38-4931-8822-0b355d8134e1";
            final String expectedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            verify(mockedStorage).set(expectedKey, expectedJson);
        }
    }

    public static class SaveSyncMetadataOfSessionsTest extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-10-16T21:30:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void saveSyncMetadataOfSessions_whenNoStoredSessionList_createsAndSavesSyncMetadataInUTC() {
            final GFSessionBundle session = new GFSessionBundle.Builder().setId("679acf3c-3d38-4931-8822-0b355d8134e1")
                .setName("short walk")
                .setApplicationIdentifier("com.google.android.apps.fitness")
                .setTimeStart(Date.from(Instant.parse("2020-10-16T11:20:00.000Z")))
                .setTimeEnd(Date.from(Instant.parse("2020-10-16T13:00:00.000Z")))
                .setActivityType("walking")
                .setType(7)
                .build();
            final List<GFSessionBundle> sessionBundles = Stream.of(session).collect(Collectors.toList());

            final String expectedSessionListKey = "gf-sync-metadata.USER_TOKEN.sessions.D16";
            when(mockedStorage.get(expectedSessionListKey)).thenReturn(null);

            gfSyncMetadataStore.saveSyncMetadataOfSessions(sessionBundles);

            // should save new metadata of the list
            String expectedListMetadataJson =
                "{\"date\":\"2020-10-16\",\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"identifiers\":[\"679acf3c-3d38-4931-8822-0b355d8134e1\"],\"schemaVersion\":1}";
            verify(mockedStorage).set(expectedSessionListKey, expectedListMetadataJson);
            // should save each session metadata
            final String expectedSessionKey = "gf-sync-metadata.USER_TOKEN.session.679acf3c-3d38-4931-8822-0b355d8134e1";
            final String expectedSessionMetadataJson =
                "{\"activitySegmentsCount\":0,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":0,\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"heartRateCount\":0,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":0,\"schemaVersion\":1,\"speedCount\":0,\"stepsCount\":0,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            verify(mockedStorage).set(expectedSessionKey, expectedSessionMetadataJson);
        }

        @Test
        public void saveSyncMetadataOfSessions_whenStoredStaleSessionList_savesNewSyncMetadataInUTC() {
            final GFSessionBundle session = new GFSessionBundle.Builder().setId("679acf3c-3d38-4931-8822-0b355d8134e1")
                .setName("short walk")
                .setApplicationIdentifier("com.google.android.apps.fitness")
                .setTimeStart(Date.from(Instant.parse("2020-10-16T11:20:00.000Z")))
                .setTimeEnd(Date.from(Instant.parse("2020-10-16T13:00:00.000Z")))
                .setActivityType("walking")
                .setType(7)
                .build();
            final List<GFSessionBundle> sessionBundles = Stream.of(session).collect(Collectors.toList());

            final String expectedSessionListKey = "gf-sync-metadata.USER_TOKEN.sessions.D16";
            final String staleListMetadataJson =
                "{\"date\":\"2020-09-16\",\"editedAt\":\"2020-09-16T21:30:00.000Z\",\"identifiers\":[\"old_session_id\"],\"schemaVersion\":1}";
            when(mockedStorage.get(expectedSessionListKey)).thenReturn(staleListMetadataJson);

            gfSyncMetadataStore.saveSyncMetadataOfSessions(sessionBundles);

            // should remove the previous session identifier
            final String staleSessionKey = "gf-sync-metadata.USER_TOKEN.session.old_session_id";
            verify(mockedStorage).set(staleSessionKey, null);

            // should save new metadata of the list
            String expectedListMetadataJson =
                "{\"date\":\"2020-10-16\",\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"identifiers\":[\"679acf3c-3d38-4931-8822-0b355d8134e1\"],\"schemaVersion\":1}";
            verify(mockedStorage).set(expectedSessionListKey, expectedListMetadataJson);
            // should save each new session metadata
            final String expectedSessionKey = "gf-sync-metadata.USER_TOKEN.session.679acf3c-3d38-4931-8822-0b355d8134e1";
            final String expectedSessionMetadataJson =
                "{\"activitySegmentsCount\":0,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":0,\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"heartRateCount\":0,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":0,\"schemaVersion\":1,\"speedCount\":0,\"stepsCount\":0,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            verify(mockedStorage).set(expectedSessionKey, expectedSessionMetadataJson);
        }

        @Test
        public void saveSyncMetadataOfSessions_whenStoredActualSessionList_savesNewSyncMetadataInUTC() {
            final GFSessionBundle session = new GFSessionBundle.Builder().setId("new_session_id")
                .setName("short walk")
                .setApplicationIdentifier("com.google.android.apps.fitness")
                .setTimeStart(Date.from(Instant.parse("2020-10-16T11:20:00.000Z")))
                .setTimeEnd(Date.from(Instant.parse("2020-10-16T13:00:00.000Z")))
                .setActivityType("walking")
                .setType(7)
                .build();
            final List<GFSessionBundle> sessionBundles = Stream.of(session).collect(Collectors.toList());

            final String expectedSessionListKey = "gf-sync-metadata.USER_TOKEN.sessions.D16";
            final String listMetadataJson =
                "{\"date\":\"2020-10-16\",\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"identifiers\":[\"other_session_id\"],\"schemaVersion\":1}";
            when(mockedStorage.get(expectedSessionListKey)).thenReturn(listMetadataJson);

            gfSyncMetadataStore.saveSyncMetadataOfSessions(sessionBundles);

            // should save the merged metadata of both lists
            String expectedListMetadataJson =
                "{\"date\":\"2020-10-16\",\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"identifiers\":[\"other_session_id\",\"new_session_id\"],\"schemaVersion\":1}";
            verify(mockedStorage).set(expectedSessionListKey, expectedListMetadataJson);
            // should save each new session metadata
            final String expectedSessionKey = "gf-sync-metadata.USER_TOKEN.session.new_session_id";
            final String expectedSessionMetadataJson =
                "{\"activitySegmentsCount\":0,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":0,\"editedAt\":\"2020-10-16T21:30:00.000Z\",\"heartRateCount\":0,\"id\":\"new_session_id\",\"name\":\"short walk\",\"powerCount\":0,\"schemaVersion\":1,\"speedCount\":0,\"stepsCount\":0,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            verify(mockedStorage).set(expectedSessionKey, expectedSessionMetadataJson);
        }
    }

    public static class CheckIfNeedToSyncCaloriesBatch extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:40:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenNoStoredMetadata_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasDifferentDate_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-08-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasDifferentTotal_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            // NOTE: there is a diff more than 0.001, the current totalKcals is 14.8147
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8248}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasDifferentPointsCount_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":2,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasNoSignificantDifference_returnsFalse() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), CALORIES_DATA_SOURCE),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), CALORIES_DATA_SOURCE)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8150}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertFalse("should not require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }
    }

    public static class CheckIfNeedToSyncHeartRateSummaryBatch extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:40:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void isNeededToSyncHRBatch_whenNoStoredMetadata_returnsTrue() {
            final GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10T10:00-D10T11:00";
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            boolean result = gfSyncMetadataStore.isNeededToSyncHRBatch(hrBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncHRBatch_whenStoredMetadataHasDifferentDate_returnsTrue() {
            final GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10RT10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-08-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"sumOfAverages\":212.41478}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncHRBatch(hrBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncHRBatch_whenStoredMetadataHasDifferentSumOfAverages_returnsTrue() {
            final GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10T10:00-D10T11:00";
            // NOTE: there is a diff more than 0.01, the current sum is 212.41478
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"sumOfAverages\":212.42479}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncHRBatch(hrBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncHRBatch_whenStoredMetadataHasDifferentPointsCount_returnsTrue() {
            final GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":2,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"sumOfAverages\":212.41478}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncHRBatch(hrBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncHRBatch_whenStoredMetadataHasNoSignificantDifference_returnsFalse() {
            final GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.hrSummaryList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.hr.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"sumOfAverages\":212.41480}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncHRBatch(hrBatch);
            assertFalse("should not require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }
    }

    public static class CheckIfNeedToSyncStepsBatch extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:40:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void isNeededToSyncStepsBatch_whenNoStoredMetadata_returnsTrue() {
            final GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            boolean result = gfSyncMetadataStore.isNeededToSyncStepsBatch(stepsBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncStepsBatch_whenStoredMetadataHasDifferentDate_returnsTrue() {
            final GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-08-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"totalSteps\":10064}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncStepsBatch(stepsBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncStepsBatch_whenStoredMetadataHasDifferentTotal_returnsTrue() {
            final GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            // NOTE: there is a diff in 1 step, the current total is 10064
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"totalSteps\":10063}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncStepsBatch(stepsBatch);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncStepsBatch_whenStoredMetadataHasDifferentPointsCount_returnsTrue() {
            final GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":2,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"totalSteps\":10064}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncStepsBatch(stepsBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncStepsBatch_whenStoredMetadataHasNoSignificantDifference_returnsFalse() {
            final GFDataPointsBatch<GFStepsDataPoint> stepsBatch = new GFDataPointsBatch<>(
                TestIntradaySamplesData.stepsList,
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.steps.D10T10:00-D10T11:00";
            final String storedJson = "{\"count\":3,\"date\":\"2020-09-10\",\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"schemaVersion\":1,\"totalSteps\":10064}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncStepsBatch(stepsBatch);
            assertFalse("should not require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }
    }

    public static class CheckIfNeedToSyncSessionBundle extends GivenRobolectricContext {
        Clock fixedClock;
        GFSyncMetadataStore gfSyncMetadataStore;
        IStorage mockedStorage;

        final GFSessionBundle session = new GFSessionBundle.Builder().setId("679acf3c-3d38-4931-8822-0b355d8134e1")
            .setName("short walk")
            .setApplicationIdentifier("com.google.android.apps.fitness")
            .setTimeStart(Date.from(Instant.parse("2020-10-16T11:20:00.000Z")))
            .setTimeEnd(Date.from(Instant.parse("2020-10-16T13:00:00.000Z")))
            .setActivityType("walking")
            .setType(7)
            .setActivitySegments(TestSessionSamplesData.activitySegments)
            .setCalories(TestSessionSamplesData.calories)
            .setHeartRate(TestSessionSamplesData.heartRate)
            .setPower(TestSessionSamplesData.power)
            .setSpeed(TestSessionSamplesData.speed)
            .setSteps(TestSessionSamplesData.steps)
            .build();
        final String expectedKey = "gf-sync-metadata.USER_TOKEN.session." + session.getId();

        @Before
        public void beforeTests() {
            String instantExpected = "2020-09-15T21:40:00Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
            mockedStorage = mock(IStorage.class);
            gfSyncMetadataStore = new GFSyncMetadataStore(mockedStorage, USER_TOKEN, fixedClock);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenNoStoredMetadata_returnsTrue() {
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentName_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"quick walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentApplicationIdentifier_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":null,\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentStartTime_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:21:05.011Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentEndTime_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.001Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentActivityType_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":17}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentSegmentsCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":1,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentCaloriesCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":2,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentStepsCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":2,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentHeartRateCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":2,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentSpeedCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":1,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasDifferentPowerCount_returnsTrue() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":0,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertTrue("should require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }

        @Test
        public void isNeededToSyncSessionBundle_whenStoredMetadataHasNoSignificantDifference_returnsFalse() {
            final String storedJson =
                "{\"activitySegmentsCount\":2,\"applicationIdentifier\":\"com.google.android.apps.fitness\",\"caloriesCount\":3,\"editedAt\":\"2020-09-15T21:40:00.000Z\",\"heartRateCount\":3,\"id\":\"679acf3c-3d38-4931-8822-0b355d8134e1\",\"name\":\"short walk\",\"powerCount\":1,\"schemaVersion\":1,\"speedCount\":2,\"stepsCount\":3,\"timeEnd\":\"2020-10-16T13:00:00.000Z\",\"timeStart\":\"2020-10-16T11:20:00.000Z\",\"type\":7}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncSessionBundle(session);
            assertFalse("should not require the sync", result);
            verify(mockedStorage, times(1)).get(expectedKey);
        }
    }
}

class TestIntradaySamplesData {
    public static final List<GFStepsDataPoint> stepsList = Arrays.asList(
        new GFStepsDataPoint(3525,
            Date.from(Instant.parse("2020-09-10T10:00:00.000Z")),
            Date.from(Instant.parse("2020-09-10T10:10:00.000Z")),
            "raw:com.google.step_count.delta:stream1"),
        new GFStepsDataPoint(5957,
            Date.from(Instant.parse("2020-09-10T10:25:01.000Z")),
            Date.from(Instant.parse("2020-09-10T10:33:25.000Z")),
            "raw:com.google.step_count.delta:stream2"),
        new GFStepsDataPoint(582,
            Date.from(Instant.parse("2020-09-10T10:35:00.000Z")),
            Date.from(Instant.parse("2020-09-10T10:59:00.000Z")),
            "raw:com.google.step_count.delta:stream1")
    );

    public static final List<GFHRSummaryDataPoint> hrSummaryList = Arrays.asList(
        new GFHRSummaryDataPoint(74.53277f, 74f, 76f, Date.from(Instant.parse("2020-09-10T10:00:00.000Z")), "raw:com.google.heart_rate.bpm:com.mc.miband1"),
        new GFHRSummaryDataPoint(73.88201f, 71f, 76f, Date.from(Instant.parse("2020-09-10T10:01:00.000Z")), "raw:com.google.heart_rate.bpm:com.mc.miband1"),
        new GFHRSummaryDataPoint(64f, 64f, 64f, Date.from(Instant.parse("2020-09-10T10:02:00.000Z")), "raw:com.google.heart_rate.bpm:com.mc.miband2")
    );

}

class TestSessionSamplesData {
    public static final List<GFHRDataPoint> heartRate = Arrays.asList(
        new GFHRDataPoint(55f,
            Date.from(Instant.parse("2020-01-15T17:25:05.000Z")),
            "raw:com.google.heart_rate.bpm:stream1"),
        new GFHRDataPoint(56f,
            Date.from(Instant.parse("2020-01-15T17:25:08.000Z")),
            "raw:com.google.heart_rate.bpm:stream1"),
        new GFHRDataPoint(58f,
            Date.from(Instant.parse("2020-01-15T17:25:10.000Z")),
            "raw:com.google.heart_rate.bpm:stream2")
    );
    public static final List<GFPowerDataPoint> power = Arrays.asList(
        new GFPowerDataPoint(10f,
            Date.from(Instant.parse("2020-01-15T17:25:05.000Z")),
            "raw:com.google.power.sample:stream")
    );
    public static final List<GFSpeedDataPoint> speed = Arrays.asList(
        new GFSpeedDataPoint(1.594202f,
            Date.from(Instant.parse("2020-01-15T17:25:08.000Z")),
            "raw:com.google.distance.delta:stream1"),
        new GFSpeedDataPoint(1.585585f,
            Date.from(Instant.parse("2020-01-15T17:25:16.000Z")),
            "raw:com.google.distance.delta:stream1")
    );
    public static final List<GFActivitySegmentDataPoint> activitySegments = Arrays.asList(
        new GFActivitySegmentDataPoint(3,
            Date.from(Instant.parse("2020-10-25T21:00:00.000Z")),
            Date.from(Instant.parse("2020-10-25T21:16:42.953Z")),
            null),
        new GFActivitySegmentDataPoint(7,
            Date.from(Instant.parse("2020-10-25T21:16:42.953Z")),
            Date.from(Instant.parse("2020-10-25T21:18:00.953Z")),
            "derived:com.google.activity.segment:com.mc.miband1:session_activity_segment")
    );
    public static final List<GFCalorieDataPoint> calories = Arrays.asList(
        new GFCalorieDataPoint(5.2751f,
            Date.from(Instant.parse("2020-01-01T10:05:00Z")),
            Date.from(Instant.parse("2020-01-01T10:15:00Z")),
            "phone"),
        new GFCalorieDataPoint(8.2698f,
            Date.from(Instant.parse("2020-01-01T10:31:00Z")),
            Date.from(Instant.parse("2020-01-01T10:46:00Z")),
            "tracker"),
        new GFCalorieDataPoint(2.5421f,
            Date.from(Instant.parse("2020-01-02T10:34:00Z")),
            Date.from(Instant.parse("2020-01-02T10:41:00Z")),
            "tracker")
    );
    public static final List<GFStepsDataPoint> steps = Arrays.asList(
        new GFStepsDataPoint(3525,
            Date.from(Instant.parse("2020-01-14T23:00:00.000Z")),
            Date.from(Instant.parse("2020-01-15T01:10:00.000Z")),
            "raw:com.google.step_count.delta:stream1"),
        new GFStepsDataPoint(582,
            Date.from(Instant.parse("2020-01-14T16:23:00.000Z")),
            Date.from(Instant.parse("2020-01-14T16:59:00.000Z")),
            "raw:com.google.step_count.delta:stream1"),
        new GFStepsDataPoint(5957,
            Date.from(Instant.parse("2020-01-15T17:25:01.000Z")),
            Date.from(Instant.parse("2020-01-15T19:33:25.000Z")),
            "raw:com.google.step_count.delta:stream2")
    );
}

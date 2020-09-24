package com.fjuul.sdk.activitysources.entities;

import android.os.Build;

import com.fjuul.sdk.entities.IStorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GFSyncMetadataStoreTest {
    final static String USER_TOKEN = "USER_TOKEN";
    public static final String dataSourceId = "derived:com.google.calories.expended:com.google.android.gms:from_activities";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {
    }

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
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), dataSourceId),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), dataSourceId),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), dataSourceId)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            gfSyncMetadataStore.saveSyncMetadataOfCalories(caloriesBatch);
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            final String expectedJson = "{\"count\":3,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            verify(mockedStorage).set(expectedKey, expectedJson);
        }
    }

    public static class GetSyncMetadataOfCaloriesTest extends GivenRobolectricContext {
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
        public void getSyncMetadataOfCalories_storageHasValueForTheKey_returnsSyncMetadata() {
            final Date startTime = Date.from(Instant.parse("2020-09-10T10:00:00Z"));
            final Date endTime = Date.from(Instant.parse("2020-09-10T11:00:00Z"));
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            final String storedJson = "{\"count\":3,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            GFSyncCaloriesMetadata metadata = gfSyncMetadataStore.getSyncMetadataOfCalories(startTime, endTime);
            assertNotNull("should return metadata", metadata);
            assertEquals("should properly deserialize json",
                metadata.getCount(),
                3);
            assertThat("should properly deserialize json",
                metadata.getEditedAt(),
                equalTo(Date.from(Instant.parse("2020-09-15T21:30:00Z"))));
            assertEquals("should properly deserialize json",
                metadata.getTotalKcals(),
                14.8147f,
                0.00001);
            assertEquals("should properly deserialize json",
                1,
                metadata.getSchemaVersion());
        }

        @Test
        public void getSyncMetadataOfCalories_storageHasNoValueForTheKey_returnsNull() {
            final Date startTime = Date.from(Instant.parse("2020-09-10T10:00:00Z"));
            final Date endTime = Date.from(Instant.parse("2020-09-10T11:00:00Z"));
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            GFSyncCaloriesMetadata metadata = gfSyncMetadataStore.getSyncMetadataOfCalories(startTime, endTime);
            assertNull("should return null", metadata);
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
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), dataSourceId)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            when(mockedStorage.get(expectedKey)).thenReturn(null);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasDifferentTotal_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), dataSourceId),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), dataSourceId),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), dataSourceId)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            final String storedJson = "{\"count\":3,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":13.5449}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasDifferentPointsCount_returnsTrue() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), dataSourceId),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), dataSourceId),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), dataSourceId)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            final String storedJson = "{\"count\":2,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertTrue("should require the sync", result);
        }

        @Test
        public void isNeededToSyncCaloriesBatch_whenStoredMetadataHasNoDifference_returnsFalse() {
            final GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = new GFDataPointsBatch<>(
                Stream.of(
                    new GFCalorieDataPoint(5.2751f, Date.from(Instant.parse("2020-09-10T10:05:00Z")), dataSourceId),
                    new GFCalorieDataPoint(1.2698f, Date.from(Instant.parse("2020-09-10T10:07:00Z")), dataSourceId),
                    new GFCalorieDataPoint(8.2698f, Date.from(Instant.parse("2020-09-10T10:31:00Z")), dataSourceId)
                ).collect(Collectors.toList()),
                Date.from(Instant.parse("2020-09-10T10:00:00Z")),
                Date.from(Instant.parse("2020-09-10T11:00:00Z"))
            );
            final String expectedKey = "gf-sync-metadata.USER_TOKEN.calories.2020-09-10T10:00-2020-09-10T11:00";
            final String storedJson = "{\"count\":3,\"editedAt\":\"2020-09-15T21:30:00.000Z\",\"schemaVersion\":1,\"totalKcals\":14.8147}";
            when(mockedStorage.get(expectedKey)).thenReturn(storedJson);
            boolean result = gfSyncMetadataStore.isNeededToSyncCaloriesBatch(caloriesBatch);
            assertFalse("should not require the sync", result);
        }
    }
}

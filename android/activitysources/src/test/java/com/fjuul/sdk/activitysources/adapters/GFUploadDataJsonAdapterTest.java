package com.fjuul.sdk.activitysources.adapters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.internal.GFUploadData;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFActivitySegmentDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFPowerSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSpeedDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import android.os.Build;

@RunWith(Enclosed.class)
public class GFUploadDataJsonAdapterTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static class ToJson extends GivenRobolectricContext {
        JsonAdapter<GFUploadData> jsonAdapter;

        public static final List<GFHRSummaryDataPoint> intradayHeartRateSummary = Arrays.asList(
            new GFHRSummaryDataPoint(74.53277f,
                74f,
                76f,
                Date.from(Instant.parse("2020-01-14T23:00:00.000Z")),
                "raw:com.google.heart_rate.bpm:com.mc.miband1"),
            new GFHRSummaryDataPoint(73.88201f,
                71f,
                76f,
                Date.from(Instant.parse("2020-01-14T23:01:00.000Z")),
                "raw:com.google.heart_rate.bpm:com.mc.miband1"),
            new GFHRSummaryDataPoint(64f,
                64f,
                64f,
                Date.from(Instant.parse("2020-01-14T23:02:00.000Z")),
                "raw:com.google.heart_rate.bpm:com.mc.miband2"),
            new GFHRSummaryDataPoint(64.37467f,
                63f,
                65f,
                Date.from(Instant.parse("2020-01-14T23:03:00.000Z")),
                "raw:com.google.heart_rate.bpm:com.mc.miband2"));
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
                "tracker"));
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
                "raw:com.google.step_count.delta:stream2"));
        public static final List<GFHRDataPoint> heartRate = Arrays.asList(
            new GFHRDataPoint(55f,
                Date.from(Instant.parse("2020-01-15T17:25:05.000Z")),
                "raw:com.google.heart_rate.bpm:stream1"),
            new GFHRDataPoint(56f,
                Date.from(Instant.parse("2020-01-15T17:25:08.000Z")),
                "raw:com.google.heart_rate.bpm:stream1"),
            new GFHRDataPoint(58f,
                Date.from(Instant.parse("2020-01-15T17:25:10.000Z")),
                "raw:com.google.heart_rate.bpm:stream2"));
        public static final List<GFPowerSummaryDataPoint> power = Arrays.asList(new GFPowerSummaryDataPoint(10f,
            Date.from(Instant.parse("2020-01-15T17:25:05.000Z")),
            "raw:com.google.power.sample:stream"));
        public static final List<GFSpeedDataPoint> speed = Arrays.asList(
            new GFSpeedDataPoint(1.594202f,
                Date.from(Instant.parse("2020-01-15T17:25:08.000Z")),
                "raw:com.google.distance.delta:stream1"),
            new GFSpeedDataPoint(1.585585f,
                Date.from(Instant.parse("2020-01-15T17:25:16.000Z")),
                "raw:com.google.distance.delta:stream1"));
        public static final List<GFActivitySegmentDataPoint> activitySegments = Arrays.asList(
            new GFActivitySegmentDataPoint(3,
                Date.from(Instant.parse("2020-10-25T21:00:00.000Z")),
                Date.from(Instant.parse("2020-10-25T21:16:42.953Z")),
                null),
            new GFActivitySegmentDataPoint(7,
                Date.from(Instant.parse("2020-10-25T21:16:42.953Z")),
                Date.from(Instant.parse("2020-10-25T21:18:00.953Z")),
                "derived:com.google.activity.segment:com.mc.miband1:session_activity_segment"));

        @Before
        public void setUp() {
            Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter())
                .add(new GFUploadDataJsonAdapter())
                .build();
            // NOTE: the json indenting is enabled only for the clearness of tests
            jsonAdapter = moshi.adapter(GFUploadData.class).indent("  ");
        }

        @Test
        public void toJson_whenOnlyIntradayCaloriesWithVariousDataSource_serializeOnlyCalories() {
            GFUploadData uploadData = new GFUploadData();
            uploadData.setCaloriesData(calories);
            String json = jsonAdapter.toJson(uploadData);
            String expectedJson = String.join(System.getProperty("line.separator"),
                "{",
                "  \"caloriesData\": [",
                "    {",
                "      \"dataSource\": \"phone\",",
                "      \"entries\": [",
                "        {",
                "          \"start\": \"2020-01-01T10:05:00.000Z\",",
                "          \"value\": 5.2751",
                "        }",
                "      ]",
                "    },",
                "    {",
                "      \"dataSource\": \"tracker\",",
                "      \"entries\": [",
                "        {",
                "          \"start\": \"2020-01-01T10:31:00.000Z\",",
                "          \"value\": 8.2698",
                "        },",
                "        {",
                "          \"start\": \"2020-01-02T10:34:00.000Z\",",
                "          \"value\": 2.5421",
                "        }",
                "      ]",
                "    }",
                "  ],",
                "  \"hrData\": [],",
                "  \"sessionsData\": [],",
                "  \"stepsData\": []",
                "}");
            assertThat("should serialize to the correct json format", json, equalTo(expectedJson));
        }

        @Test
        public void toJson_whenOnlyIntradayStepsWithVariousDataSource_serializeOnlySteps() {
            GFUploadData uploadData = new GFUploadData();
            uploadData.setStepsData(steps);
            String json = jsonAdapter.toJson(uploadData);
            String expectedJson = String.join(System.getProperty("line.separator"),
                "{",
                "  \"caloriesData\": [],",
                "  \"hrData\": [],",
                "  \"sessionsData\": [],",
                "  \"stepsData\": [",
                "    {",
                "      \"dataSource\": \"raw:com.google.step_count.delta:stream2\",",
                "      \"entries\": [",
                "        {",
                "          \"start\": \"2020-01-15T17:25:01.000Z\",",
                "          \"value\": 5957",
                "        }",
                "      ]",
                "    },",
                "    {",
                "      \"dataSource\": \"raw:com.google.step_count.delta:stream1\",",
                "      \"entries\": [",
                "        {",
                "          \"start\": \"2020-01-14T23:00:00.000Z\",",
                "          \"value\": 3525",
                "        },",
                "        {",
                "          \"start\": \"2020-01-14T16:23:00.000Z\",",
                "          \"value\": 582",
                "        }",
                "      ]",
                "    }",
                "  ]",
                "}");
            assertThat("should serialize to the correct json format", json, equalTo(expectedJson));
        }

        @Test
        public void toJson_whenOnlyIntradayHeartRateWithVariousDataSource_serializeOnlyHR() {
            GFUploadData uploadData = new GFUploadData();
            uploadData.setHrData(intradayHeartRateSummary);
            String json = jsonAdapter.toJson(uploadData);
            String expectedJson = String.join(System.getProperty("line.separator"),
                "{",
                "  \"caloriesData\": [],",
                "  \"hrData\": [",
                "    {",
                "      \"dataSource\": \"raw:com.google.heart_rate.bpm:com.mc.miband2\",",
                "      \"entries\": [",
                "        {",
                "          \"avg\": 64.0,",
                "          \"max\": 64.0,",
                "          \"min\": 64.0,",
                "          \"start\": \"2020-01-14T23:02:00.000Z\"",
                "        },",
                "        {",
                "          \"avg\": 64.37467,",
                "          \"max\": 65.0,",
                "          \"min\": 63.0,",
                "          \"start\": \"2020-01-14T23:03:00.000Z\"",
                "        }",
                "      ]",
                "    },",
                "    {",
                "      \"dataSource\": \"raw:com.google.heart_rate.bpm:com.mc.miband1\",",
                "      \"entries\": [",
                "        {",
                "          \"avg\": 74.53277,",
                "          \"max\": 76.0,",
                "          \"min\": 74.0,",
                "          \"start\": \"2020-01-14T23:00:00.000Z\"",
                "        },",
                "        {",
                "          \"avg\": 73.88201,",
                "          \"max\": 76.0,",
                "          \"min\": 71.0,",
                "          \"start\": \"2020-01-14T23:01:00.000Z\"",
                "        }",
                "      ]",
                "    }",
                "  ],",
                "  \"sessionsData\": [],",
                "  \"stepsData\": []",
                "}");
            assertThat("should serialize to the correct json format", json, equalTo(expectedJson));
        }

        @Test
        public void toJson_whenOnlySessionsData_serializeOnlySessions() {
            List<GFSessionBundle> sessions = Arrays.asList(new GFSessionBundle.Builder().setId("123")
                .setName("nice-ride")
                .setApplicationIdentifier("com.google.android.apps.fitness")
                .setTimeStart(Date.from(Instant.parse("2020-01-16T11:20:00.000Z")))
                .setTimeEnd(Date.from(Instant.parse("2020-01-16T13:00:00.000Z")))
                .setActivityType("biking")
                .setType(1)
                .setActivitySegments(activitySegments)
                .setCalories(calories)
                .setHeartRate(heartRate)
                .setPower(power)
                .setSpeed(speed)
                .setSteps(steps)
                .build());
            GFUploadData uploadData = new GFUploadData();
            uploadData.setSessionsData(sessions);
            String json = jsonAdapter.toJson(uploadData);
            String expectedJson = String.join(System.getProperty("line.separator"),
                "{",
                "  \"caloriesData\": [],",
                "  \"hrData\": [],",
                "  \"sessionsData\": [",
                "    {",
                "      \"activitySegments\": [",
                "        {",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-10-25T21:16:42.953Z\",",
                "              \"start\": \"2020-10-25T21:00:00.000Z\",",
                "              \"value\": 3",
                "            }",
                "          ]",
                "        },",
                "        {",
                "          \"dataSource\": \"derived:com.google.activity.segment:com.mc.miband1:session_activity_segment\",",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-10-25T21:18:00.953Z\",",
                "              \"start\": \"2020-10-25T21:16:42.953Z\",",
                "              \"value\": 7",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"applicationIdentifier\": \"com.google.android.apps.fitness\",",
                "      \"calories\": [",
                "        {",
                "          \"dataSource\": \"phone\",",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-01-01T10:15:00.000Z\",",
                "              \"start\": \"2020-01-01T10:05:00.000Z\",",
                "              \"value\": 5.2751",
                "            }",
                "          ]",
                "        },",
                "        {",
                "          \"dataSource\": \"tracker\",",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-01-01T10:46:00.000Z\",",
                "              \"start\": \"2020-01-01T10:31:00.000Z\",",
                "              \"value\": 8.2698",
                "            },",
                "            {",
                "              \"end\": \"2020-01-02T10:41:00.000Z\",",
                "              \"start\": \"2020-01-02T10:34:00.000Z\",",
                "              \"value\": 2.5421",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"heartRate\": [",
                "        {",
                "          \"dataSource\": \"raw:com.google.heart_rate.bpm:stream1\",",
                "          \"entries\": [",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:05.000Z\",",
                "              \"value\": 55.0",
                "            },",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:08.000Z\",",
                "              \"value\": 56.0",
                "            }",
                "          ]",
                "        },",
                "        {",
                "          \"dataSource\": \"raw:com.google.heart_rate.bpm:stream2\",",
                "          \"entries\": [",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:10.000Z\",",
                "              \"value\": 58.0",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"id\": \"123\",",
                "      \"name\": \"nice-ride\",",
                "      \"power\": [",
                "        {",
                "          \"dataSource\": \"raw:com.google.power.sample:stream\",",
                "          \"entries\": [",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:05.000Z\",",
                "              \"value\": 10.0",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"speed\": [",
                "        {",
                "          \"dataSource\": \"raw:com.google.distance.delta:stream1\",",
                "          \"entries\": [",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:08.000Z\",",
                "              \"value\": 1.594202",
                "            },",
                "            {",
                "              \"timestamp\": \"2020-01-15T17:25:16.000Z\",",
                "              \"value\": 1.585585",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"steps\": [",
                "        {",
                "          \"dataSource\": \"raw:com.google.step_count.delta:stream2\",",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-01-15T19:33:25.000Z\",",
                "              \"start\": \"2020-01-15T17:25:01.000Z\",",
                "              \"value\": 5957",
                "            }",
                "          ]",
                "        },",
                "        {",
                "          \"dataSource\": \"raw:com.google.step_count.delta:stream1\",",
                "          \"entries\": [",
                "            {",
                "              \"end\": \"2020-01-15T01:10:00.000Z\",",
                "              \"start\": \"2020-01-14T23:00:00.000Z\",",
                "              \"value\": 3525",
                "            },",
                "            {",
                "              \"end\": \"2020-01-14T16:59:00.000Z\",",
                "              \"start\": \"2020-01-14T16:23:00.000Z\",",
                "              \"value\": 582",
                "            }",
                "          ]",
                "        }",
                "      ],",
                "      \"timeEnd\": \"2020-01-16T13:00:00.000Z\",",
                "      \"timeStart\": \"2020-01-16T11:20:00.000Z\",",
                "      \"type\": 1",
                "    }",
                "  ],",
                "  \"stepsData\": []",
                "}");
            assertThat("should serialize to the correct json format", json, equalTo(expectedJson));
        }
    }
}

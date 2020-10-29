package com.fjuul.sdk.activitysources.json;

import java.util.Date;
import java.util.List;

public class GFUploadDataJson {
    final List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData;
    final List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData;
    final List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData;
    final List<GFSessionJson> sessionsData;

    public GFUploadDataJson(List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData,
                            List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData,
                            List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData,
                            List<GFSessionJson> sessionsData) {
        this.caloriesData = caloriesData;
        this.stepsData = stepsData;
        this.hrData = hrData;
        this.sessionsData = sessionsData;
    }

    public static class GFSampleJson<TEntry> {
        final String dataSource;
        final List<TEntry> entries;

        public GFSampleJson(String dataSource, List<TEntry> entries) {
            this.dataSource = dataSource;
            this.entries = entries;
        }
    }

    public static class GFSessionJson {
        final private String id;
        final private String name;
        final private String applicationIdentifier;
        final private Date timeStart;
        final private Date timeEnd;
        final private int type;
        final private List<GFSampleJson<GFSampleEntryJson<Integer>>> activitySegments;
        final private List<GFSampleJson<GFSampleEntryJson<Float>>> calories;
        final private List<GFSampleJson<GFSampleEntryJson<Integer>>> steps;
        final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speed;
        final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> heartRate;
        final private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> power;

        public GFSessionJson(String id, String name, String applicationIdentifier, Date timeStart,
                             Date timeEnd, int type,
                             List<GFSampleJson<GFSampleEntryJson<Integer>>> activitySegments,
                             List<GFSampleJson<GFSampleEntryJson<Float>>> calories,
                             List<GFSampleJson<GFSampleEntryJson<Integer>>> steps,
                             List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speed,
                             List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> heartRate,
                             List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> power) {
            this.id = id;
            this.name = name;
            this.applicationIdentifier = applicationIdentifier;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.type = type;
            this.activitySegments = activitySegments;
            this.calories = calories;
            this.steps = steps;
            this.speed = speed;
            this.heartRate = heartRate;
            this.power = power;
        }
    }

    public static class GFIntradaySampleEntryJson<TValue extends Number> {
        final Date start;
        final TValue value;

        public GFIntradaySampleEntryJson(Date start, TValue value) {
            this.start = start;
            this.value = value;
        }
    }

    public static class GFIntradayHRSampleEntryJson {
        final Date start;
        final float avg;
        final float min;
        final float max;

        public GFIntradayHRSampleEntryJson(Date start, float avg, float min, float max) {
            this.start = start;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }

    public static class GFSampleEntryJson<TValue extends Number> {
        final Date start;
        final Date end;
        final TValue value;

        public GFSampleEntryJson(Date start, Date end, TValue value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    public static class GFInstantMeasureSampleEntryJson<TValue extends Number> {
        final Date timestamp;
        final TValue value;

        public GFInstantMeasureSampleEntryJson(Date timestamp, TValue value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}

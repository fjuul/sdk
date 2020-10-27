package com.fjuul.sdk.activitysources.json;

import java.util.Date;
import java.util.List;

public class GFUploadDataJson {
    List<GFSampleJson<GFIntradaySampleEntryJson<Float>>> caloriesData;
    List<GFSampleJson<GFIntradaySampleEntryJson<Integer>>> stepsData;
    List<GFSampleJson<GFIntradayHRSampleEntryJson>> hrData;
    List<GFSessionJson> sessionsData;

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
        String dataSource;
        List<TEntry> entries;

        public GFSampleJson(String dataSource, List<TEntry> entries) {
            this.dataSource = dataSource;
            this.entries = entries;
        }
    }

    public static class GFSessionJson {
        private String id;
        private String name;
        private String applicationIdentifier;
        private Date timeStart;
        private Date timeEnd;
        private int type;
        private List<GFSampleJson<GFSampleEntryJson<Float>>> calories;
        private List<GFSampleJson<GFSampleEntryJson<Integer>>> steps;
        private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> speed;
        private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> heartRate;
        private List<GFSampleJson<GFInstantMeasureSampleEntryJson<Float>>> power;

        public GFSessionJson(String id, String name, String applicationIdentifier, Date timeStart,
                             Date timeEnd, int type,
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
            this.calories = calories;
            this.steps = steps;
            this.speed = speed;
            this.heartRate = heartRate;
            this.power = power;
        }
    }

    public static class GFIntradaySampleEntryJson<TValue extends Number> {
        Date start;
        TValue value;

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
        Date start;
        Date end;
        TValue value;

        public GFSampleEntryJson(Date start, Date end, TValue value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    public static class GFInstantMeasureSampleEntryJson<TValue extends Number> {
        Date timestamp;
        TValue value;

        public GFInstantMeasureSampleEntryJson(Date timestamp, TValue value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}

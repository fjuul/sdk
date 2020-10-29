package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class GFSessionBundle {
    @NonNull private String id;

    @Nullable private String name;

    @Nullable private String applicationIdentifier;

    @NonNull private Date timeStart;

    @NonNull private Date timeEnd;

    // string presentation of activity
    @NonNull private transient String activityType;

    private int type;

    @NonNull private List<GFActivitySegmentDataPoint> activitySegments;

    @NonNull private List<GFCalorieDataPoint> calories;

    @NonNull private List<GFStepsDataPoint> steps;

    @NonNull private List<GFHRDataPoint> heartRate;

    @NonNull private List<GFPowerDataPoint> power;

    @NonNull private List<GFSpeedDataPoint> speed;

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getApplicationIdentifier() {
        return applicationIdentifier;
    }

    @NonNull
    public Date getTimeStart() {
        return timeStart;
    }

    @NonNull
    public Date getTimeEnd() {
        return timeEnd;
    }

    @NonNull
    public String getActivityType() {
        return activityType;
    }

    public int getType() {
        return type;
    }

    @NonNull
    public List<GFActivitySegmentDataPoint> getActivitySegments() {
        return activitySegments;
    }

    @NonNull
    public List<GFCalorieDataPoint> getCalories() {
        return calories;
    }

    @NonNull
    public List<GFStepsDataPoint> getSteps() {
        return steps;
    }

    @NonNull
    public List<GFHRDataPoint> getHeartRate() {
        return heartRate;
    }

    @NonNull
    public List<GFPowerDataPoint> getPower() {
        return power;
    }

    @NonNull
    public List<GFSpeedDataPoint> getSpeed() {
        return speed;
    }

    private GFSessionBundle(@NonNull String id,
                            @Nullable String name,
                            @Nullable String applicationIdentifier,
                            @NonNull Date timeStart,
                            @NonNull Date timeEnd,
                            @NonNull String activityType,
                            int type,
                            @NonNull List<GFActivitySegmentDataPoint> activitySegments,
                            @NonNull List<GFCalorieDataPoint> calories,
                            @NonNull List<GFStepsDataPoint> steps,
                            @NonNull List<GFHRDataPoint> heartRate,
                            @NonNull List<GFPowerDataPoint> power,
                            @NonNull List<GFSpeedDataPoint> speed) {
        this.id = id;
        this.name = name;
        this.applicationIdentifier = applicationIdentifier;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.activityType = activityType;
        this.type = type;
        this.activitySegments = activitySegments;
        this.calories = calories;
        this.steps = steps;
        this.heartRate = heartRate;
        this.power = power;
        this.speed = speed;
    }

    @SuppressLint("NewApi")
    @Override
    public String toString() {
        return "GFSessionBundle{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", applicationIdentifier='" + applicationIdentifier + '\'' +
            ", timeStart=" + timeStart +
            ", timeEnd=" + timeEnd +
            ", activityType='" + activityType + '\'' +
            ", type=" + type +
            ", activitySegments= size " + Optional.ofNullable(activitySegments).map(List::size) +
            ", calories= size " + Optional.ofNullable(calories).map(List::size) +
            ", steps= size " + Optional.ofNullable(steps).map(List::size) +
            ", heartRate= size " + Optional.ofNullable(heartRate).map(List::size) +
            ", power= size " + Optional.ofNullable(power).map(List::size) +
            ", speed= size " + Optional.ofNullable(speed).map(List::size) +
            '}';
    }

    public static class Builder {
        @NonNull private String id;
        @Nullable private String name;
        @Nullable private String applicationIdentifier;
        @NonNull private Date timeStart;
        @NonNull private Date timeEnd;
        @NonNull private String activityType;
        private int type;

        @Nullable private List<GFActivitySegmentDataPoint> activitySegments;
        @Nullable private List<GFCalorieDataPoint> calories;
        @Nullable private List<GFStepsDataPoint> steps;
        @Nullable private List<GFHRDataPoint> heartRate;
        @Nullable private List<GFPowerDataPoint> power;
        @Nullable private List<GFSpeedDataPoint> speed;

        public Builder setId(@NonNull String id) {
            this.id = id;
            return this;
        }

        public Builder setName(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder setApplicationIdentifier(@Nullable String applicationIdentifier) {
            this.applicationIdentifier = applicationIdentifier;
            return this;
        }

        public Builder setTimeStart(@NonNull Date timeStart) {
            this.timeStart = timeStart;
            return this;
        }

        public Builder setTimeEnd(@NonNull Date timeEnd) {
            this.timeEnd = timeEnd;
            return this;
        }

        public Builder setActivityType(@NonNull String activityType) {
            this.activityType = activityType;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setActivitySegments(@Nullable List<GFActivitySegmentDataPoint> activitySegments) {
            this.activitySegments = activitySegments;
            return this;
        }

        public Builder setCalories(@Nullable List<GFCalorieDataPoint> calories) {
            this.calories = calories;
            return this;
        }

        public Builder setSteps(@Nullable List<GFStepsDataPoint> steps) {
            this.steps = steps;
            return this;
        }

        public Builder setHeartRate(@Nullable List<GFHRDataPoint> heartRate) {
            this.heartRate = heartRate;
            return this;
        }

        public Builder setPower(@Nullable List<GFPowerDataPoint> power) {
            this.power = power;
            return this;
        }

        public Builder setSpeed(@Nullable List<GFSpeedDataPoint> speed) {
            this.speed = speed;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public GFSessionBundle build() {
            // TODO: validate fields ?
            return new GFSessionBundle(id, name, applicationIdentifier, timeStart, timeEnd, activityType, type,
                Optional.ofNullable(activitySegments).orElse(Collections.emptyList()),
                Optional.ofNullable(calories).orElse(Collections.emptyList()),
                Optional.ofNullable(steps).orElse(Collections.emptyList()),
                Optional.ofNullable(heartRate).orElse(Collections.emptyList()),
                Optional.ofNullable(power).orElse(Collections.emptyList()),
                Optional.ofNullable(speed).orElse(Collections.emptyList()));
        }
    }
}

package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class GFSessionBundle {
    private String id;

    private String name;

    @Nullable private String applicationIdentifier;

    private Date timeStart;

    private Date timeEnd;

    // string presentation of activity
    private transient String activityType;

    private int type;

    private List<GFCalorieDataPoint> calories;

    private List<GFStepsDataPoint> steps;

    private List<GFHRDataPoint> heartRate;

    private List<GFPowerDataPoint> power;

    private List<GFSpeedDataPoint> speed;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getApplicationIdentifier() {
        return applicationIdentifier;
    }

    public Date getTimeStart() {
        return timeStart;
    }

    public Date getTimeEnd() {
        return timeEnd;
    }

    public String getActivityType() {
        return activityType;
    }

    public int getType() {
        return type;
    }

    public List<GFCalorieDataPoint> getCalories() {
        return calories;
    }

    public List<GFStepsDataPoint> getSteps() {
        return steps;
    }

    public List<GFHRDataPoint> getHeartRate() {
        return heartRate;
    }

    public List<GFPowerDataPoint> getPower() {
        return power;
    }

    public List<GFSpeedDataPoint> getSpeed() {
        return speed;
    }

    private GFSessionBundle(String id, String name, @Nullable String applicationIdentifier, Date timeStart, Date timeEnd, String activityType,
                           int type, List<GFCalorieDataPoint> calories, List<GFStepsDataPoint> steps,
                           List<GFHRDataPoint> heartRate, List<GFPowerDataPoint> power, List<GFSpeedDataPoint> speed) {
        this.id = id;
        this.name = name;
        this.applicationIdentifier = applicationIdentifier;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.activityType = activityType;
        this.type = type;
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
            ", calories= size " + Optional.ofNullable(calories).map(List::size) +
            ", steps= size " + Optional.ofNullable(steps).map(List::size) +
            ", heartRate= size " + Optional.ofNullable(heartRate).map(List::size) +
            ", power= size " + Optional.ofNullable(power).map(List::size) +
            ", speed= size " + Optional.ofNullable(speed).map(List::size) +
            '}';
    }

    public static class Builder {
        private String id;

        private String name;

        @Nullable private String applicationIdentifier;

        private Date timeStart;

        private Date timeEnd;

        private String activityType;

        private int type;

        private List<GFCalorieDataPoint> calories;

        private List<GFStepsDataPoint> steps;

        private List<GFHRDataPoint> heartRate;

        private List<GFPowerDataPoint> power;

        private List<GFSpeedDataPoint> speed;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setApplicationIdentifier(@Nullable String applicationIdentifier) {
            this.applicationIdentifier = applicationIdentifier;
            return this;
        }

        public Builder setTimeStart(Date timeStart) {
            this.timeStart = timeStart;
            return this;
        }

        public Builder setTimeEnd(Date timeEnd) {
            this.timeEnd = timeEnd;
            return this;
        }

        public Builder setActivityType(String activityType) {
            this.activityType = activityType;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setCalories(List<GFCalorieDataPoint> calories) {
            this.calories = calories;
            return this;
        }

        public Builder setSteps(List<GFStepsDataPoint> steps) {
            this.steps = steps;
            return this;
        }

        public Builder setHeartRate(List<GFHRDataPoint> heartRate) {
            this.heartRate = heartRate;
            return this;
        }

        public Builder setPower(List<GFPowerDataPoint> power) {
            this.power = power;
            return this;
        }

        public Builder setSpeed(List<GFSpeedDataPoint> speed) {
            this.speed = speed;
            return this;
        }

        public GFSessionBundle build() {
            // TODO: validate fields
            return new GFSessionBundle(id, name, applicationIdentifier, timeStart, timeEnd, activityType, type, calories, steps, heartRate, power, speed);
        }
    }
}

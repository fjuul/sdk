package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GFSessionBundle {
    @NonNull
    private String id;

    @Nullable
    private String name;

    @Nullable
    private String applicationIdentifier;

    @NonNull
    private Date timeStart;

    @NonNull
    private Date timeEnd;

    // string presentation of activity
    // please keep `transient` for moshi
    @NonNull
    private transient String activityType;

    private int type;

    @NonNull
    private List<GFActivitySegmentDataPoint> activitySegments;

    @NonNull
    private List<GFCalorieDataPoint> calories;

    @NonNull
    private List<GFStepsDataPoint> steps;

    @NonNull
    private List<GFHRDataPoint> heartRate;

    @NonNull
    private List<GFPowerDataPoint> power;

    @NonNull
    private List<GFSpeedDataPoint> speed;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GFSessionBundle that = (GFSessionBundle) o;

        if (type != that.type) return false;
        if (!id.equals(that.id)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (applicationIdentifier != null ? !applicationIdentifier.equals(that.applicationIdentifier)
            : that.applicationIdentifier != null) return false;
        if (!timeStart.equals(that.timeStart)) return false;
        if (!timeEnd.equals(that.timeEnd)) return false;
        if (!activityType.equals(that.activityType)) return false;
        if (!activitySegments.equals(that.activitySegments)) return false;
        if (!calories.equals(that.calories)) return false;
        if (!steps.equals(that.steps)) return false;
        if (!heartRate.equals(that.heartRate)) return false;
        if (!power.equals(that.power)) return false;
        return speed.equals(that.speed);
    }

    @SuppressLint("NewApi")
    @Override
    @NonNull
    public String toString() {
        //@formatter:off
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
        //@formatter:on
    }

    public static class Builder {
        @NonNull
        private String id;
        @Nullable
        private String name;
        @Nullable
        private String applicationIdentifier;
        @NonNull
        private Date timeStart;
        @NonNull
        private Date timeEnd;
        @NonNull
        private String activityType;
        private int type;

        @Nullable
        private List<GFActivitySegmentDataPoint> activitySegments;
        @Nullable
        private List<GFCalorieDataPoint> calories;
        @Nullable
        private List<GFStepsDataPoint> steps;
        @Nullable
        private List<GFHRDataPoint> heartRate;
        @Nullable
        private List<GFPowerDataPoint> power;
        @Nullable
        private List<GFSpeedDataPoint> speed;

        @NonNull
        public Builder setId(@NonNull String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder setName(@Nullable String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder setApplicationIdentifier(@Nullable String applicationIdentifier) {
            this.applicationIdentifier = applicationIdentifier;
            return this;
        }

        @NonNull
        public Builder setTimeStart(@NonNull Date timeStart) {
            this.timeStart = timeStart;
            return this;
        }

        @NonNull
        public Builder setTimeEnd(@NonNull Date timeEnd) {
            this.timeEnd = timeEnd;
            return this;
        }

        @NonNull
        public Builder setActivityType(@NonNull String activityType) {
            this.activityType = activityType;
            return this;
        }

        @NonNull
        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        @NonNull
        public Builder setActivitySegments(@Nullable List<GFActivitySegmentDataPoint> activitySegments) {
            this.activitySegments = activitySegments;
            return this;
        }

        @NonNull
        public Builder setCalories(@Nullable List<GFCalorieDataPoint> calories) {
            this.calories = calories;
            return this;
        }

        @NonNull
        public Builder setSteps(@Nullable List<GFStepsDataPoint> steps) {
            this.steps = steps;
            return this;
        }

        @NonNull
        public Builder setHeartRate(@Nullable List<GFHRDataPoint> heartRate) {
            this.heartRate = heartRate;
            return this;
        }

        @NonNull
        public Builder setPower(@Nullable List<GFPowerDataPoint> power) {
            this.power = power;
            return this;
        }

        @NonNull
        public Builder setSpeed(@Nullable List<GFSpeedDataPoint> speed) {
            this.speed = speed;
            return this;
        }

        @SuppressLint("NewApi")
        @NonNull
        public GFSessionBundle build() {
            return new GFSessionBundle(id,
                name,
                applicationIdentifier,
                timeStart,
                timeEnd,
                activityType,
                type,
                Optional.ofNullable(activitySegments).orElse(Collections.emptyList()),
                Optional.ofNullable(calories).orElse(Collections.emptyList()),
                Optional.ofNullable(steps).orElse(Collections.emptyList()),
                Optional.ofNullable(heartRate).orElse(Collections.emptyList()),
                Optional.ofNullable(power).orElse(Collections.emptyList()),
                Optional.ofNullable(speed).orElse(Collections.emptyList()));
        }
    }
}

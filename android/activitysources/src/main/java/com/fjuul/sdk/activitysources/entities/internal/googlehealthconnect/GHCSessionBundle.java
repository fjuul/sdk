package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GHCSessionBundle {
    @NonNull
    private String id;

    @Nullable
    private String title;

    @Nullable
    private String notes;

    @NonNull
    private Date timeStart;

    @NonNull
    private Date timeEnd;

    private int type;

    @NonNull
    private List<GHCActivitySegmentDataPoint> activitySegments;

    @NonNull
    private List<GHCCalorieDataPoint> calories;

    @NonNull
    private List<GHCStepsDataPoint> steps;

    @NonNull
    private List<GHCHeartRateSummaryDataPoint> heartRate;

    @NonNull
    private List<GHCPowerSummaryDataPoint> power;

    @NonNull
    private List<GHCSpeedSummaryDataPoint> speed;

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    @NonNull
    public Date getTimeStart() {
        return timeStart;
    }

    @NonNull
    public Date getTimeEnd() {
        return timeEnd;
    }

    public int getType() {
        return type;
    }

    @NonNull
    public List<GHCActivitySegmentDataPoint> getActivitySegments() {
        return activitySegments;
    }

    @NonNull
    public List<GHCCalorieDataPoint> getCalories() {
        return calories;
    }

    @NonNull
    public List<GHCStepsDataPoint> getSteps() {
        return steps;
    }

    @NonNull
    public List<GHCHeartRateSummaryDataPoint> getHeartRate() {
        return heartRate;
    }

    @NonNull
    public List<GHCPowerSummaryDataPoint> getPower() {
        return power;
    }

    @NonNull
    public List<GHCSpeedSummaryDataPoint> getSpeed() {
        return speed;
    }

    public GHCSessionBundle(@Nullable String title,
        @Nullable String notes,
        @NonNull Date timeStart,
        @NonNull Date timeEnd,
        int type,
        @NonNull List<GHCActivitySegmentDataPoint> activitySegments,
        @NonNull List<GHCCalorieDataPoint> calories,
        @NonNull List<GHCStepsDataPoint> steps,
        @NonNull List<GHCHeartRateSummaryDataPoint> heartRate,
        @NonNull List<GHCPowerSummaryDataPoint> power,
        @NonNull List<GHCSpeedSummaryDataPoint> speed) {
        this.id = generateId(timeStart, timeEnd, title, type);
        this.title = title;
        this.notes = notes;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
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

        final GHCSessionBundle that = (GHCSessionBundle) o;

        if (!id.equals(that.id)) return false;
        if (type != that.type) return false;
        if (!Objects.equals(title, that.title)) return false;
        if (!Objects.equals(notes, that.notes)) return false;
        if (!timeStart.equals(that.timeStart)) return false;
        if (!timeEnd.equals(that.timeEnd)) return false;
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
        return "GHCSessionBundle{" +
            "title='" + title + '\'' +
            ", notes='" + notes + '\'' +
            ", timeStart=" + timeStart +
            ", timeEnd=" + timeEnd +
            ", type=" + type +
            ", activitySegments= size " + activitySegments.size() +
            ", calories= size " + calories.size() +
            ", steps= size " + steps.size() +
            ", heartRate= size " + heartRate.size() +
            ", power= size " + power.size() +
            '}';
        //@formatter:on
    }

    /**
     * Generate a hopefully unique identifier using session's data. It would make little sense to have two sessions
     * covering the exact same period with the exact same title and exercise type.
     *
     * @param timeStart the start of the session
     * @param timeEnd the end of the session
     * @param title an optional title for the session
     * @param exerciseType the type of exercise
     * @return unique ID for the session
     */
    private static String generateId(@NonNull Date timeStart,
        @NonNull Date timeEnd,
        @Nullable String title,
        int exerciseType) {
        long tmp = timeStart.getTime() + timeEnd.getTime() + exerciseType;
        if (title != null) {
            for (char ch : title.toCharArray()) {
                tmp += ch;
            }
        }
        return Long.toString(tmp);
    }
}

package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCHeartRateDataPoint extends HCDataPoint {
    public enum Type {
        INTRADAY,
        RESTING
    }

    private final float heartRate;
    private final Type type;

    public HCHeartRateDataPoint(@NonNull Date start, @Nullable Date end, float heartRate, Type type, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.heartRate = heartRate;
        this.type = type;
    }

    public static HCHeartRateDataPoint createIntraday(@NonNull Date start, @NonNull Date end, float heartRate, @Nullable String dataSource) {
        return new HCHeartRateDataPoint(start, end, heartRate, Type.INTRADAY, dataSource);
    }

    public static HCHeartRateDataPoint createResting(@NonNull Date timestamp, float heartRate, @Nullable String dataSource) {
        return new HCHeartRateDataPoint(timestamp, null, heartRate, Type.RESTING, dataSource);
    }

    public float getHeartRate() {
        return heartRate;
    }

    public Type getType() {
        return type;
    }

    public boolean isIntraday() {
        return type == Type.INTRADAY;
    }

    public boolean isResting() {
        return type == Type.RESTING;
    }
} 
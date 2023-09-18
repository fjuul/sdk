package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GHCSpeedSummaryDataPoint extends GHCDataPoint {
    protected final double avg;
    protected final double min;
    protected final double max;

    public double getAvg() {
        return avg;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public GHCSpeedSummaryDataPoint(double avg,
        double min,
        double max,
        @NonNull Date start,
        @NonNull Date end,
        @NonNull String dataSource) {
        super(start, end, dataSource);
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        final String endFormatted = dateFormat.format(end);
        return String.format(Locale.getDefault(),
            "GHCSpeedSummaryDataPoint: avg speed %f, min speed %f, max speed %f, start %s, end %s, dataSource %s",
            avg,
            min,
            max,
            startFormatted,
            endFormatted,
            dataSource);
    }
}

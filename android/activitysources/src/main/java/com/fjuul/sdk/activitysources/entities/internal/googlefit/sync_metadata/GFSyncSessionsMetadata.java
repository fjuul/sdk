package com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class GFSyncSessionsMetadata extends GFSyncDatedEntityMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    @NonNull
    private final List<String> identifiers;

    public GFSyncSessionsMetadata(@NonNull List<String> identifiers, @NonNull LocalDate date, @NonNull Date editedAt) {
        super(CURRENT_SCHEMA_VERSION, date, editedAt);
        this.identifiers = identifiers;
    }

    @NonNull
    public List<String> getIdentifiers() {
        return identifiers;
    }

    @SuppressLint("NewApi")
    public static GFSyncSessionsMetadata buildFromList(@NonNull List<GFSessionBundle> sessionBundleList,
        @NonNull Clock clock) {
        final Date start = sessionBundleList.get(0).getTimeStart();
        final LocalDate date = start.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        final List<String> identifiers =
            sessionBundleList.stream().map(GFSessionBundle::getId).collect(Collectors.toList());
        final Date editedAt = Date.from(clock.instant());
        return new GFSyncSessionsMetadata(identifiers, date, editedAt);
    }
}

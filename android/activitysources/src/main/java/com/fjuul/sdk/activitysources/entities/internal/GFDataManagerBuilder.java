package com.fjuul.sdk.activitysources.entities.internal;

import java.util.Date;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GFDataManagerBuilder {
    @NonNull
    private final Context context;
    @NonNull
    private final GFDataUtils gfDataUtils;
    @NonNull
    private final GFSyncMetadataStore syncMetadataStore;
    @NonNull
    private final ActivitySourcesService sourcesService;

    public GFDataManagerBuilder(@NonNull Context context,
        @NonNull GFDataUtils gfDataUtils,
        @NonNull GFSyncMetadataStore syncMetadataStore,
        @NonNull ActivitySourcesService sourcesService) {
        this.context = context;
        this.gfDataUtils = gfDataUtils;
        this.syncMetadataStore = syncMetadataStore;
        this.sourcesService = sourcesService;
    }

    @NonNull
    public GFDataManager build(@NonNull GoogleSignInAccount account, @Nullable Date lowerDateBoundary) {
        final HistoryClient historyClient = Fitness.getHistoryClient(context, account);
        final SessionsClient sessionsClient = Fitness.getSessionsClient(context, account);
        final GFClientWrapper clientWrapper = new GFClientWrapper(historyClient, sessionsClient, gfDataUtils);
        return new GFDataManager(clientWrapper, gfDataUtils, syncMetadataStore, sourcesService, lowerDateBoundary);
    }
}

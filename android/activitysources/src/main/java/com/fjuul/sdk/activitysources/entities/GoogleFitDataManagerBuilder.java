package com.fjuul.sdk.activitysources.entities;

import android.content.Context;

import androidx.annotation.NonNull;

import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;

class GoogleFitDataManagerBuilder {
    @NonNull
    private final Context context;
    @NonNull
    private final GFDataUtils gfDataUtils;
    @NonNull
    private final GFSyncMetadataStore syncMetadataStore;
    @NonNull
    private final ActivitySourcesService sourcesService;

    public GoogleFitDataManagerBuilder(@NonNull Context context,
                                       @NonNull GFDataUtils gfDataUtils,
                                       @NonNull GFSyncMetadataStore syncMetadataStore,
                                       @NonNull ActivitySourcesService sourcesService) {
        this.context = context;
        this.gfDataUtils = gfDataUtils;
        this.syncMetadataStore = syncMetadataStore;
        this.sourcesService = sourcesService;
    }

    public GoogleFitDataManager build(@NonNull GoogleSignInAccount account) {
        final HistoryClient historyClient = Fitness.getHistoryClient(context, account);
        final SessionsClient sessionsClient = Fitness.getSessionsClient(context, account);
        final GFClientWrapper clientWrapper = new GFClientWrapper(historyClient, sessionsClient, gfDataUtils);
        return new GoogleFitDataManager(clientWrapper, gfDataUtils, syncMetadataStore, sourcesService);
    }
}

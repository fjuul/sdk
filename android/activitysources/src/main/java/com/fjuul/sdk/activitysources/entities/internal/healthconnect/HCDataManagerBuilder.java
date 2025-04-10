package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import android.content.Context;

import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.storage.Storage;

public class HCDataManagerBuilder {
    private final Context context;
    private final HCDataUtils dataUtils;
    private final ActivitySourcesService sourcesService;

    public HCDataManagerBuilder(@NonNull Context context, @NonNull HCDataUtils dataUtils,
        @NonNull ActivitySourcesService sourcesService) {
        this.context = context;
        this.dataUtils = dataUtils;
        this.sourcesService = sourcesService;
    }

    @NonNull
    public HCDataManager build() {
        return new HCDataManager(context);
    }
} 
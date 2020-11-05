package com.fjuul.sdk.activitysources.entities;

import android.content.Intent;

public abstract class ActivitySource {
    protected abstract Intent buildIntentRequestingPermissions();
}

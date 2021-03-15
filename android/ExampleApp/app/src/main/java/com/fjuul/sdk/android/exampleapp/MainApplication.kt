package com.fjuul.sdk.android.exampleapp

import android.app.Application
import com.fjuul.sdk.core.utils.DebugTimberTree
import timber.log.Timber

public class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTimberTree())
        }
    }
}

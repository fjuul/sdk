package com.fjuul.sdk.android.exampleapp

import androidx.multidex.MultiDexApplication
import com.fjuul.sdk.core.utils.DebugFjuulSDKTimberTree
import timber.log.Timber

public class MainApplication: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugFjuulSDKTimberTree())
        }
    }
}

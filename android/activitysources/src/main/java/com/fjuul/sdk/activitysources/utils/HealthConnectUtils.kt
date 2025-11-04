package com.fjuul.sdk.activitysources.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient

fun isHealthConnectAvailable(context: Context): Boolean {
    return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}

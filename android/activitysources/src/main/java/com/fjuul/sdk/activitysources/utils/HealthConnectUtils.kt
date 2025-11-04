package com.fjuul.sdk.activitysources.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectAvailability

fun getHealthConnectAvailability(context: Context): HealthConnectAvailability {
    return when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.SDK_AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        else -> HealthConnectAvailability.SDK_UNAVAILABLE
    }
}

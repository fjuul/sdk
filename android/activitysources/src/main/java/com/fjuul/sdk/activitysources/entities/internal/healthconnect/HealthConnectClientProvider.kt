package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
import androidx.health.connect.client.HealthConnectClient.Companion.getSdkStatus

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

object HealthConnectClientProvider {

    fun getClient(context: Context): HealthConnectClient? {
        return if (isHealthConnectAvailable(context)) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    fun getAvailability(context: Context): HealthConnectAvailability {
        return when (getSdkStatus(context)) {
            SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
            SDK_UNAVAILABLE -> HealthConnectAvailability.NOT_SUPPORTED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    private fun isHealthConnectAvailable(context: Context): Boolean {
        return getSdkStatus(context) == SDK_AVAILABLE
    }
}

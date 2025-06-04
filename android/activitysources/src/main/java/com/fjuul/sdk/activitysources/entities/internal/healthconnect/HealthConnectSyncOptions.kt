package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType

/**
 * Configuration class defining what Health Connect data to sync
 *
 * This object is passed from ViewModel/UI to ActivitySource.
 */
data class HealthConnectSyncOptions(
    val metrics: Set<FitnessMetricsType>
)

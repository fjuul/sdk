package com.fjuul.sdk.activitysources.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.UserCredentials

abstract class HCSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    protected val activitySourcesManager: ActivitySourcesManager
        get() {
            var sourcesManager: ActivitySourcesManager
            try {
                sourcesManager = ActivitySourcesManager.getInstance()
            } catch (_: IllegalStateException) {
                val userToken = inputData.getString(KEY_USER_TOKEN_ARG) ?: EMPTY
                val userSecret = inputData.getString(KEY_USER_SECRET_ARG) ?: EMPTY
                val apiKey = inputData.getString(KEY_API_KEY_ARG) ?: EMPTY
                val baseUrl = inputData.getString(KEY_BASE_URL_ARG) ?: EMPTY
                val client = ApiClient.Builder(applicationContext, baseUrl, apiKey)
                    .setUserCredentials(UserCredentials(userToken, userSecret))
                    .build()
                val config =
                    ActivitySourcesManagerConfig.Builder().keepUntouchedHealthConnectBackgroundSync()
                        .setCollectableHCFitnessMetrics(mutableSetOf<FitnessMetricsType?>())
                        .build(false)
                ActivitySourcesManager.initialize(client, config, false)
                sourcesManager = ActivitySourcesManager.getInstance()
            }
            return sourcesManager
        }

    protected fun getHealthConnectActivitySourceConnection(
        manager: ActivitySourcesManager
    ): ActivitySourceConnection? {
        val currentConnections = manager.current
        return currentConnections.stream()
            .filter { connection: ActivitySourceConnection? -> connection?.activitySource is HealthConnectActivitySource }
            .findFirst()
            .orElse(null)
    }

    companion object {
        const val KEY_USER_TOKEN_ARG: String = "USER_TOKEN"
        const val KEY_USER_SECRET_ARG: String = "USER_SECRET"
        const val KEY_API_KEY_ARG: String = "API_KEY"
        const val KEY_BASE_URL_ARG: String = "BASE_URL"
        const val EMPTY: String = ""
    }
}

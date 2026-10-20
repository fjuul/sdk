package com.fjuul.sdk.activitysources.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource

abstract class HCSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    // Null when the active instance belongs to another session than the one that queued this work.
    protected fun getSessionActivitySourcesManager(): ActivitySourcesManager? =
        ActivitySourcesManager.getOrInitializeForWorker(applicationContext, inputData)

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
    }
}

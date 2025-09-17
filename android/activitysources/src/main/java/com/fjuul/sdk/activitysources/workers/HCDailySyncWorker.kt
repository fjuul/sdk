package com.fjuul.sdk.activitysources.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.core.utils.Logger
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

class HCDailySyncWorker(context: Context, workerParams: WorkerParameters) :
    HCSyncWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val hcConnection = getHealthConnectActivitySourceConnection(activitySourcesManager)
        if (hcConnection == null) {
            return Result.success()
        }
        val hcSource = (hcConnection.activitySource as HealthConnectActivitySource)
        val permissionManager = hcSource.getPermissionManager()

        if (permissionManager.isBackgroundPermissionGranted()) {
            val taskCompletionSource = TaskCompletionSource<Void?>()
            val syncOptions = buildDailySyncOptions()

            hcSource.syncDaily(syncOptions) { result ->
                if (result.isError && result.error is Exception) {
                    taskCompletionSource.trySetException(result.error as Exception)
                    return@syncDaily
                }
                taskCompletionSource.trySetResult(null)
            }
            try {
                Tasks.await<Void?>(taskCompletionSource.getTask())
                return Result.success()
            } catch (_: Exception) {
            }
        }
        return Result.failure()
    }

    private fun buildDailySyncOptions(): HealthConnectSyncOptions {
        val metrics = mutableSetOf<FitnessMetricsType>()
        val rawDailyMetrics = inputData.getStringArray(KEY_HC_DAILY_METRICS) ?: emptyArray()
        for (rawDailyMetric in rawDailyMetrics) {
            try {
                val metric = FitnessMetricsType.valueOf(rawDailyMetric)
                metrics.add(metric)
            } catch (e: Exception) {
                Logger.get().e(e, "Exception during daily sync")
            }
        }
        return HealthConnectSyncOptions(metrics)
    }

    companion object {
        const val KEY_HC_DAILY_METRICS: String = "HC_DAILY_METRICS"
    }
}

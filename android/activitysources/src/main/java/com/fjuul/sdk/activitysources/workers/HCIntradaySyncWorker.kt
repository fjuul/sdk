package com.fjuul.sdk.activitysources.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.core.utils.Logger
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

class HCIntradaySyncWorker(context: Context, workerParams: WorkerParameters) :
    HCSyncWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val hcConnection = getHealthConnectActivitySourceConnection(activitySourcesManager)
        if (hcConnection == null) {
            return Result.success()
        }
        val hcSource = (hcConnection.activitySource as HealthConnectActivitySource)
        val permissionManager = hcSource.getPermissionManager()

        if (permissionManager.checkBackgroundPermission()) {
            val taskCompletionSource = TaskCompletionSource<Void?>()
            val syncOptions = buildIntradaySyncOptions()
            hcSource.syncIntraday(syncOptions) { result ->
                if (result.isError && result.error is Exception) {
                    taskCompletionSource.trySetException(result.error as Exception)
                    return@syncIntraday
                }
                taskCompletionSource.trySetResult(null)
            }

            try {
                Tasks.await<Void?>(taskCompletionSource.getTask())
                return Result.success()
            } catch (e: Exception) {
                Logger.get().e(e, "Exception during intraday sync")
            }
        }
        return Result.failure()
    }

    private fun buildIntradaySyncOptions(): HealthConnectSyncOptions {
        val metrics = mutableSetOf<FitnessMetricsType>()
        val rawIntradayMetrics = inputData.getStringArray(KEY_HC_INTRADAY_METRICS) ?: emptyArray()
        for (rawIntradayMetric in rawIntradayMetrics) {
            try {
                val metric = FitnessMetricsType.valueOf(rawIntradayMetric)
                metrics.add(metric)
            } catch (_: Exception) {
            }
        }
        return HealthConnectSyncOptions(metrics)
    }

    companion object {
        const val KEY_HC_INTRADAY_METRICS: String = "HC_INTRADAY_METRICS"
    }
}

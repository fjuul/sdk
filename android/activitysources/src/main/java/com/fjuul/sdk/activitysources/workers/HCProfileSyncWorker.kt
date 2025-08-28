package com.fjuul.sdk.activitysources.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

class HCProfileSyncWorker(context: Context, workerParams: WorkerParameters) :
    HCSyncWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val hcConnection = getHealthConnectActivitySourceConnection(activitySourcesManager)
        if (hcConnection == null) {
            return Result.success()
        }
        val hcSource = (hcConnection.activitySource as HealthConnectActivitySource)
        val taskCompletionSource = TaskCompletionSource<Boolean?>()
        val syncOptions = buildProfileSyncOptions()
        hcSource.syncProfile(syncOptions) { result ->
            if (result.isError && result.error is Exception) {
                taskCompletionSource.trySetException(result.error as Exception)
                return@syncProfile
            }
            taskCompletionSource.trySetResult(null)
        }
        try {
            Tasks.await<Boolean?>(taskCompletionSource.getTask())
            return Result.success()
        } catch (_: Exception) {
        }
        return Result.failure()
    }

    private fun buildProfileSyncOptions(): HealthConnectSyncOptions {
        val metrics = mutableSetOf<FitnessMetricsType>()
        val rawProfileMetrics = inputData.getStringArray(KEY_HC_PROFILE_METRICS) ?: emptyArray()
        for (rawProfileMetric in rawProfileMetrics) {
            try {
                val metric = FitnessMetricsType.valueOf(rawProfileMetric)
                metrics.add(metric)
            } catch (_: Exception) {
            }
        }
        return HealthConnectSyncOptions(metrics)
    }

    companion object {
        const val KEY_HC_PROFILE_METRICS: String = "HC_PROFILE_METRICS"
    }
}

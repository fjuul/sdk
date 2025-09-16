package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions

class HealthConnectSyncViewModel : ViewModel() {
    private val _syncingIntradayData = MutableLiveData(false)
    private val _syncingDailyData = MutableLiveData(false)
    private val _syncingProfileData = MutableLiveData(false)
    private val _errorMessage = MutableLiveData<String?>()

    val syncingIntradayData: LiveData<Boolean> = _syncingIntradayData
    val syncingDailyData: LiveData<Boolean> = _syncingDailyData
    val syncingProfileData: LiveData<Boolean> = _syncingProfileData
    val errorMessage: LiveData<String?> = _errorMessage

    fun resetErrorMessage() {
        _errorMessage.postValue(null)
    }

    fun runIntradaySync(calories: Boolean, heartRate: Boolean) {
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (calories) metricsToTrack.add(FitnessMetricsType.INTRADAY_CALORIES)
        if (heartRate) metricsToTrack.add(FitnessMetricsType.INTRADAY_HEART_RATE)

        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack
        )

        _syncingIntradayData.value = true

        (connection.activitySource as HealthConnectActivitySource)
            .syncIntraday(options) { result ->
                _syncingIntradayData.postValue(false)
                if (result.isError) {
                    _errorMessage.postValue(
                        result.error?.message ?: "Unknown error during sync"
                    )
                }
            }
    }

    fun runDailySync(steps: Boolean, restingHeartRate: Boolean) {
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (steps) metricsToTrack.add(FitnessMetricsType.STEPS)
        if (restingHeartRate) metricsToTrack.add(FitnessMetricsType.RESTING_HEART_RATE)

        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack
        )

        _syncingDailyData.value = true

        (connection.activitySource as HealthConnectActivitySource)
            .syncDaily(options) { result ->
                _syncingDailyData.postValue(false)
                if (result.isError) {
                    _errorMessage.postValue(
                        result.error?.message ?: "Unknown error during sync"
                    )
                }
            }
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (weight) metricsToTrack.add(FitnessMetricsType.WEIGHT)
        if (height) metricsToTrack.add(FitnessMetricsType.HEIGHT)

        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack
        )

        _syncingProfileData.value = true

        (connection.activitySource as HealthConnectActivitySource)
            .syncProfile(options) { result ->
                _syncingProfileData.postValue(false)
                if (result.isError) {
                    _errorMessage.postValue(
                        result.error?.message ?: "Unknown error during sync"
                    )
                }
            }
    }

    fun clearAllChangesTokens() {
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        (connection.activitySource as HealthConnectActivitySource)
            .forInternalUseOnly_clearChangesTokens()
    }
}

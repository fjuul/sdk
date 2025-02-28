package com.fjuul.sdk.android.exampleapp.ui.hc_sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.HealthConnectIntradaySyncOptions
import com.fjuul.sdk.activitysources.entities.HealthConnectProfileSyncOptions

class HCSyncViewModel : ViewModel() {
    private val _syncingIntradayMetrics = MutableLiveData(false)
    private val _syncingProfile = MutableLiveData(false)
    private val _errorMessage = MutableLiveData<String?>()

    val syncingIntradayMetrics: LiveData<Boolean> = _syncingIntradayMetrics
    val syncingProfile: LiveData<Boolean> = _syncingProfile
    val errorMessage: LiveData<String?> = _errorMessage

    fun resetErrorMessage() {
        _errorMessage.postValue(null)
    }

    fun runIntradaySync(calories: Boolean, hr: Boolean, steps: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val hcConnectionSource = manager.current.find { connection -> connection.activitySource is HealthConnectActivitySource }
        if (hcConnectionSource == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }
        lateinit var options: HealthConnectIntradaySyncOptions
        try {
            options = HealthConnectIntradaySyncOptions.Builder().apply {
                if (calories) { include(FitnessMetricsType.INTRADAY_CALORIES) }
                if (hr) { include(FitnessMetricsType.INTRADAY_HEART_RATE) }
                if (steps) { include(FitnessMetricsType.INTRADAY_STEPS) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }

        _syncingIntradayMetrics.postValue(true)
        (hcConnectionSource.activitySource as HealthConnectActivitySource).syncIntradayMetrics(options) { result ->
            _syncingIntradayMetrics.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncIntradayMetrics
            }
        }
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val hcConnectionSource = manager.current.find { connection -> connection.activitySource is HealthConnectActivitySource }
        if (hcConnectionSource == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }
        lateinit var options: HealthConnectProfileSyncOptions
        try {
            options = HealthConnectProfileSyncOptions.Builder().apply {
                if (height) { include(FitnessMetricsType.HEIGHT) }
                if (weight) { include(FitnessMetricsType.WEIGHT) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }
        _syncingProfile.postValue(true)
        (hcConnectionSource.activitySource as HealthConnectActivitySource).syncProfile(options) { result ->
            _syncingProfile.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncProfile
            }
        }
    }
}

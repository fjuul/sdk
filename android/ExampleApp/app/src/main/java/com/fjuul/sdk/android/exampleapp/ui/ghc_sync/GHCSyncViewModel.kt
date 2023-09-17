package com.fjuul.sdk.android.exampleapp.ui.ghc_sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectIntradaySyncOptions
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectProfileSyncOptions
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectSessionSyncOptions
import java.time.Duration

class GHCSyncViewModel : ViewModel() {
    private val _syncingIntradayMetrics = MutableLiveData(false)
    private val _syncingSessions = MutableLiveData(false)
    private val _syncingProfile = MutableLiveData(false)
    private val _errorMessage = MutableLiveData<String?>()

    val syncingIntradayMetrics: LiveData<Boolean> = _syncingIntradayMetrics
    val syncingSessions: LiveData<Boolean> = _syncingSessions
    val syncingProfile: LiveData<Boolean> = _syncingProfile
    val errorMessage: LiveData<String?> = _errorMessage

    fun resetErrorMessage() {
        _errorMessage.postValue(null)
    }

    fun runIntradaySync(calories: Boolean, hr: Boolean, steps: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val ghcConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleHealthConnectActivitySource }
        if (ghcConnectionSource == null) {
            _errorMessage.value = "No active Google Health Connect connection"
            return
        }
        lateinit var options: GoogleHealthConnectIntradaySyncOptions
        try {
            options = GoogleHealthConnectIntradaySyncOptions.Builder().apply {
                if (calories) { include(FitnessMetricsType.INTRADAY_CALORIES) }
                if (hr) { include(FitnessMetricsType.INTRADAY_HEART_RATE) }
                if (steps) { include(FitnessMetricsType.INTRADAY_STEPS) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }

        _syncingIntradayMetrics.postValue(true)
        (ghcConnectionSource.activitySource as GoogleHealthConnectActivitySource).syncIntradayMetrics(options) { result ->
            _syncingIntradayMetrics.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncIntradayMetrics
            }
        }
    }

    fun runSessionsSync(minSessionDuration: Duration) {
        val manager = ActivitySourcesManager.getInstance()
        val ghcConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleHealthConnectActivitySource }
        if (ghcConnectionSource == null) {
            _errorMessage.value = "No active Google Health Connect connection"
            return
        }
        lateinit var options: GoogleHealthConnectSessionSyncOptions
        try {
            options = GoogleHealthConnectSessionSyncOptions.Builder()
                .setMinimumSessionDuration(minSessionDuration)
                .build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }

        _syncingSessions.postValue(true)
        (ghcConnectionSource.activitySource as GoogleHealthConnectActivitySource).syncSessions(options) { result ->
            _syncingSessions.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncSessions
            }
        }
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val ghcConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleHealthConnectActivitySource }
        if (ghcConnectionSource == null) {
            _errorMessage.value = "No active Google Health Connect connection"
            return
        }
        lateinit var options: GoogleHealthConnectProfileSyncOptions
        try {
            options = GoogleHealthConnectProfileSyncOptions.Builder().apply {
                if (height) { include(FitnessMetricsType.HEIGHT) }
                if (weight) { include(FitnessMetricsType.WEIGHT) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }
        _syncingProfile.postValue(true)
        (ghcConnectionSource.activitySource as GoogleHealthConnectActivitySource).syncProfile(options) { result ->
            _syncingProfile.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncProfile
            }
        }
    }
}

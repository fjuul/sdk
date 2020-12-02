package com.fjuul.sdk.android.exampleapp.ui.gf_sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.GFIntradaySyncOptions
import com.fjuul.sdk.activitysources.entities.GFSessionSyncOptions
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource
import java.time.Duration
import java.time.LocalDate

class GFSyncViewModel : ViewModel() {
    private val _startDate = MutableLiveData<LocalDate>(LocalDate.now())
    private val _endDate = MutableLiveData<LocalDate>(LocalDate.now())
    private val _syncingIntradayMetrics = MutableLiveData<Boolean>(false)
    private val _syncingSessions = MutableLiveData<Boolean>(false)
    private val _errorMessage = MutableLiveData<String?>()

    val startDate: LiveData<LocalDate> = _startDate
    val endDate: LiveData<LocalDate> = _endDate
    val syncingIntradayMetrics: LiveData<Boolean> = _syncingIntradayMetrics
    val syncingSessions: LiveData<Boolean> = _syncingSessions
    val errorMessage: LiveData<String?> = _errorMessage

    fun setupDateRange(startDate: LocalDate? = null, endDate: LocalDate? = null) {
        if (startDate != null) {
            _startDate.postValue(startDate)
        }
        if (endDate != null) {
            _endDate.postValue(endDate)
        }
    }

    fun resetErrorMessage() {
        _errorMessage.postValue(null)
    }

    fun runIntradaySync(calories: Boolean, hr: Boolean, steps: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val gfConnectionSource = manager.current?.find { connection -> connection.activitySource is GoogleFitActivitySource }
        if (gfConnectionSource == null) {
            _errorMessage.value = "No active Google Fit connection"
            return
        }
        val options = GFIntradaySyncOptions.Builder().apply {
            setDateRange(_startDate.value!!, _endDate.value!!)
            if (calories) { include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES) }
            if (hr) { include(GFIntradaySyncOptions.METRICS_TYPE.HEART_RATE) }
            if (steps) { include(GFIntradaySyncOptions.METRICS_TYPE.STEPS) }
        }.build()
        _syncingIntradayMetrics.postValue(true)
        (gfConnectionSource.activitySource as GoogleFitActivitySource).syncIntradayMetrics(options) { result ->
            _syncingIntradayMetrics.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncIntradayMetrics
            }
        }
    }

    fun runSessionsSync(minSessionDuration: Duration) {
        val manager = ActivitySourcesManager.getInstance()
        val gfConnectionSource = manager.current?.find { connection -> connection.activitySource is GoogleFitActivitySource }
        if (gfConnectionSource == null) {
            _errorMessage.value = "No active Google Fit connection"
            return
        }
        val options = GFSessionSyncOptions.Builder()
            .setDateRange(_startDate.value!!, _endDate.value!!)
            .setMinimumSessionDuration(minSessionDuration)
            .build()
        _syncingSessions.postValue(true)
        (gfConnectionSource.activitySource as GoogleFitActivitySource).syncSessions(options) { result ->
            _syncingSessions.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncSessions
            }
        }
    }
}

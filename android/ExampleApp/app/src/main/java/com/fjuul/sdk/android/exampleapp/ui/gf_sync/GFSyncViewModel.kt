package com.fjuul.sdk.android.exampleapp.ui.gf_sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions
import com.fjuul.sdk.activitysources.entities.GoogleFitSessionSyncOptions
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate

class GFSyncViewModel : ViewModel() {
    //_startDate and _endDate giving error for null so LocalDate removed
    private val _startDate = MutableLiveData(LocalDate.now())
    private val _endDate = MutableLiveData(LocalDate.now())
    private val _syncingIntradayMetrics = MutableLiveData<Boolean>(false)
    private val _syncingSessions = MutableLiveData<Boolean>(false)
    private val _syncingProfile = MutableLiveData<Boolean>(false)
    private val _errorMessage = MutableLiveData<String?>()

    val startDate: LiveData<LocalDate> = _startDate
    val endDate: LiveData<LocalDate> = _endDate
    val syncingIntradayMetrics: LiveData<Boolean> = _syncingIntradayMetrics
    val syncingSessions: LiveData<Boolean> = _syncingSessions
    val syncingProfile: LiveData<Boolean> = _syncingProfile
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
        val gfConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleFitActivitySource }
        if (gfConnectionSource == null) {
            _errorMessage.value = "No active Google Fit connection"
            return
        }
        lateinit var options: GoogleFitIntradaySyncOptions
        try {
            options = GoogleFitIntradaySyncOptions.Builder().apply {
                setDateRange(_startDate.value!!, _endDate.value!!)
                if (calories) { include(FitnessMetricsType.INTRADAY_CALORIES) }
                if (hr) { include(FitnessMetricsType.INTRADAY_HEART_RATE) }
                if (steps) { include(FitnessMetricsType.INTRADAY_STEPS) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }

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
        val gfConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleFitActivitySource }
        if (gfConnectionSource == null) {
            _errorMessage.value = "No active Google Fit connection"
            return
        }
        lateinit var options: GoogleFitSessionSyncOptions
        try {
            options = GoogleFitSessionSyncOptions.Builder()
                .setDateRange(_startDate.value!!, _endDate.value!!)
                .setMinimumSessionDuration(minSessionDuration)
                .build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }

        _syncingSessions.postValue(true)
        (gfConnectionSource.activitySource as GoogleFitActivitySource).syncSessions(options) { result ->
            _syncingSessions.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncSessions
            }
        }
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val gfConnectionSource = manager.current.find { connection -> connection.activitySource is GoogleFitActivitySource }
        if (gfConnectionSource == null) {
            _errorMessage.value = "No active Google Fit connection"
            return
        }
        lateinit var options: GoogleFitProfileSyncOptions
        try {
            options = GoogleFitProfileSyncOptions.Builder().apply {
                if (height) { include(FitnessMetricsType.HEIGHT) }
                if (weight) { include(FitnessMetricsType.WEIGHT) }
            }.build()
        } catch (exc: Exception) {
            _errorMessage.postValue(exc.message)
            return
        }
        _syncingProfile.postValue(true)
        (gfConnectionSource.activitySource as GoogleFitActivitySource).syncProfile(options) { result ->
            _syncingProfile.postValue(false)
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@syncProfile
            }
        }
    }
}

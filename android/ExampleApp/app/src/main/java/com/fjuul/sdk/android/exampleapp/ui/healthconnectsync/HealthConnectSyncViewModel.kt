package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.Duration
import java.time.LocalDate

class HealthConnectSyncViewModel : ViewModel() {
    private val _startDate = MutableLiveData(LocalDate.now())
    private val _endDate = MutableLiveData(LocalDate.now())
    private val _syncingIntradayMetrics = MutableLiveData(false)
    private val _syncingSessions = MutableLiveData(false)
    private val _syncingProfile = MutableLiveData(false)
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

    fun runIntradaySync(calories: Boolean, hr: Boolean, steps: Boolean, distance: Boolean) {
        // TODO: Implement Health Connect sync logic
        _syncingIntradayMetrics.postValue(true)
        // Simulate sync
        _syncingIntradayMetrics.postValue(false)
    }

    fun runSessionsSync(minSessionDuration: Duration) {
        // TODO: Implement Health Connect sessions sync logic
        _syncingSessions.postValue(true)
        // Simulate sync
        _syncingSessions.postValue(false)
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        // TODO: Implement Health Connect profile sync logic
        _syncingProfile.postValue(true)
        // Simulate sync
        _syncingProfile.postValue(false)
    }
}

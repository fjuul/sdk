package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class HealthConnectSyncViewModel : ViewModel() {
    private val _startDate = MutableLiveData(LocalDate.now())
    private val _endDate = MutableLiveData(LocalDate.now())
    private val _syncingIntradayData = MutableLiveData(false)
    private val _syncingDailyData = MutableLiveData(false)
    private val _syncingProfileData = MutableLiveData(false)
    private val _errorMessage = MutableLiveData<String?>()

    val startDate: LiveData<LocalDate> = _startDate
    val endDate: LiveData<LocalDate> = _endDate
    val syncingIntradayData: LiveData<Boolean> = _syncingIntradayData
    val syncingDailyData: LiveData<Boolean> = _syncingDailyData
    val syncingProfileData: LiveData<Boolean> = _syncingProfileData
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

    fun runIntradaySync(calories: Boolean, heartRate: Boolean) {
        // TODO: Implement Health Connect  Intraday sync logic
    }

    fun runDailySync(steps: Boolean, restingHeartRate: Boolean) {
        // TODO: Implement Health Connect Daily sync logic
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        // TODO: Implement Health Connect profile sync logic
    }
}

package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

import androidx.lifecycle.viewModelScope
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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
        val manager = ActivitySourcesManager.getInstance()
        val connection = manager.current.find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }
        val source = connection.activitySource as HealthConnectActivitySource
        val start = _startDate.value!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = _endDate.value!!.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        _syncingIntradayData.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                source.uploadIntradayData(start, end)
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            } finally {
                _syncingIntradayData.postValue(false)
            }
        }
    }

    fun runDailySync(steps: Boolean, restingHeartRate: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val connection = manager.current.find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }
        val source = connection.activitySource as HealthConnectActivitySource
        val startDate = _startDate.value!!
        val endDate = _endDate.value!!
        _syncingDailyData.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var date = startDate
                while (!date.isAfter(endDate)) {
                    source.uploadDailyData(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    date = date.plusDays(1)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            } finally {
                _syncingDailyData.postValue(false)
            }
        }
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        val manager = ActivitySourcesManager.getInstance()
        val connection = manager.current.find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }
        val source = connection.activitySource as HealthConnectActivitySource
        _syncingProfileData.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                source.uploadProfileData()
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            } finally {
                _syncingProfileData.postValue(false)
            }
        }
    }
}

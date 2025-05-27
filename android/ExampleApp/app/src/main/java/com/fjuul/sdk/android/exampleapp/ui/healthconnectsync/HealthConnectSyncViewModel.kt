package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.HealthConnectActivitySource
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.HealthConnectSyncOptions
import com.fjuul.sdk.core.entities.Callback
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
        // 1) Check active connection
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        // 2) Build epoch-millis time range from LiveData<LocalDate>
        val start = _startDate.value ?: run {
            _errorMessage.value = "Start date is not set"
            return
        }
        val end = _endDate.value ?: run {
            _errorMessage.value = "End date is not set"
            return
        }

        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (calories) metricsToTrack.add(FitnessMetricsType.INTRADAY_CALORIES)
        if (heartRate) metricsToTrack.add(FitnessMetricsType.INTRADAY_HEART_RATE)

        // 3) Construct sync options
        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack,
            timeRangeStart = start,
            timeRangeEnd = end
        )

        // 4) Signal UI
        _syncingIntradayData.value = true

        // 5) Call SDK
        (connection.activitySource as HealthConnectActivitySource)
            .syncIntraday(options, object : Callback<Unit> {
                override fun onResult(result: com.fjuul.sdk.core.entities.Result<Unit>) {
                    // always back on main
                    _syncingIntradayData.postValue(false)
                    if (result.isError) {
                        _errorMessage.postValue(
                            result.error?.message ?: "Unknown error during sync"
                        )
                    }
                }
            })
    }

    fun runDailySync(steps: Boolean, restingHeartRate: Boolean) {
        // 1) Check active connection
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        // 2) Build epoch-millis time range from LiveData<LocalDate>
        val start = _startDate.value ?: run {
            _errorMessage.value = "Start date is not set"
            return
        }
        val end = _endDate.value ?: run {
            _errorMessage.value = "End date is not set"
            return
        }

        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (steps) metricsToTrack.add(FitnessMetricsType.STEPS)
        if (restingHeartRate) metricsToTrack.add(FitnessMetricsType.INTRADAY_HEART_RATE)

        // 3) Construct sync options
        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack,
            timeRangeStart = start,
            timeRangeEnd = end
        )

        // 4) Signal UI
        _syncingDailyData.value = true

        // 5) Call SDK
        (connection.activitySource as HealthConnectActivitySource)
            .syncDaily(options, object : Callback<Unit> {
                override fun onResult(result: com.fjuul.sdk.core.entities.Result<Unit>) {
                    // always back on main
                    _syncingDailyData.postValue(false)
                    if (result.isError) {
                        _errorMessage.postValue(
                            result.error?.message ?: "Unknown error during sync"
                        )
                    }
                }
            })
    }

    fun runProfileSync(height: Boolean, weight: Boolean) {
        // 1) Check active connection
        val connection = ActivitySourcesManager.getInstance().current
            .find { it.activitySource is HealthConnectActivitySource }
        if (connection == null) {
            _errorMessage.value = "No active Health Connect connection"
            return
        }

        // 2) Build epoch-millis time range from LiveData<LocalDate>
        val start = _startDate.value ?: run {
            _errorMessage.value = "Start date is not set"
            return
        }
        val end = _endDate.value ?: run {
            _errorMessage.value = "End date is not set"
            return
        }
        val metricsToTrack = mutableSetOf<FitnessMetricsType>()
        if (weight) metricsToTrack.add(FitnessMetricsType.WEIGHT)
        if (height) metricsToTrack.add(FitnessMetricsType.HEIGHT)

        // 3) Construct sync options
        val options = HealthConnectSyncOptions(
            metrics = metricsToTrack,
            timeRangeStart = start,
            timeRangeEnd = end
        )

        // 4) Signal UI
        _syncingProfileData.value = true

        // 5) Call SDK
        (connection.activitySource as HealthConnectActivitySource)
            .syncProfile(options, object : Callback<Unit> {
                override fun onResult(result: com.fjuul.sdk.core.entities.Result<Unit>) {
                    // always back on main
                    _syncingProfileData.postValue(false)
                    if (result.isError) {
                        _errorMessage.postValue(
                            result.error?.message ?: "Unknown error during sync"
                        )
                    }
                }
            })
    }
}

package com.fjuul.sdk.android.exampleapp.ui.daily_stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.analytics.entities.DailyStats
import com.fjuul.sdk.analytics.http.services.AnalyticsService
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import java.time.LocalDate

class DailyStatsViewModel() : ViewModel() {
    private val analyticsService = AnalyticsService(ApiClientHolder.sdkClient)
    private val _startDate = MutableLiveData<LocalDate>(LocalDate.now())
    private val _endDate = MutableLiveData<LocalDate>(LocalDate.now())

    private val _data = MutableLiveData<Array<DailyStats>>(arrayOf())

    val data: LiveData<Array<DailyStats>> = _data
    val startDate: LiveData<LocalDate> = _startDate
    val endDate: LiveData<LocalDate> = _endDate

    fun requestData() {
        analyticsService.getDailyStats(_startDate.value.toString(), _endDate.value.toString())
            .enqueue { _, result ->
                if (result.isError) {
                    _data.postValue(arrayOf())
                } else {
                    _data.postValue(result.value!!)
                }
            }
    }

    fun setupDateRange(startDate: LocalDate? = null, endDate: LocalDate? = null) {
        if (startDate != null) {
            _startDate.value = startDate
        }
        if (endDate != null) {
            _endDate.value = endDate
        }
        requestData()
    }
}

/**
 * ViewModel provider factory to instantiate OnboardingViewModel.
 * Required given OnboardingViewModel has a non-empty constructor
 */
class DailyStatsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyStatsViewModel::class.java)) {
            return DailyStatsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

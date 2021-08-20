package com.fjuul.sdk.android.exampleapp.ui.aggregated_daily_stats
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.analytics.entities.AggregatedDailyStats
import com.fjuul.sdk.analytics.entities.AggregationType
import com.fjuul.sdk.analytics.http.services.AnalyticsService
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import java.time.LocalDate

class AggregatedDailyStatsViewModel() : ViewModel() {
    private val analyticsService = AnalyticsService(ApiClientHolder.sdkClient)
    private val _startDate = MutableLiveData<LocalDate>(LocalDate.now())
    private val _endDate = MutableLiveData<LocalDate>(LocalDate.now())
    private val _aggregation = MutableLiveData<AggregationType>(AggregationType.sum)
    private val _data = MutableLiveData<AggregatedDailyStats>()
    private val _errorMessage = MutableLiveData<String>()

    val data: LiveData<AggregatedDailyStats> = _data
    val startDate: LiveData<LocalDate> = _startDate
    val endDate: LiveData<LocalDate> = _endDate
    val aggregation: LiveData<AggregationType> = _aggregation
    val errorMessage: LiveData<String> = _errorMessage

    fun requestData() {
        Log.d("xz", (_aggregation.value).toString())
        analyticsService.getAggregatedDailyStats(_startDate.value!!, _endDate.value!!, _aggregation.value!!)
            .enqueue { _, result ->
                Log.d("rezzzzzzzult", result.toString())
                if (result.isError) {
                    _errorMessage.postValue(result.error?.message)
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

    fun setAggregation(aggregation: AggregationType) {
        _aggregation.value = aggregation
        requestData()
    }
}

/**
 * ViewModel provider factory to instantiate AggregatedDailyStatsViewModel.
 * Required given AggregatedDailyStatsViewModelFactory has a non-empty constructor
 */
class AggregatedDailyStatsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AggregatedDailyStatsViewModel::class.java)) {
            return AggregatedDailyStatsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

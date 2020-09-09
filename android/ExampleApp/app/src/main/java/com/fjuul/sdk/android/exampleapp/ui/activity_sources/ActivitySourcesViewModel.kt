package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ConnectionResult
import com.fjuul.sdk.activitysources.entities.TrackerConnection
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder

class ActivitySourcesViewModel() : ViewModel() {
    private val activitySourcesService = ActivitySourcesService(ApiClientHolder.sdkClient)
    private val _currentConnections = MutableLiveData<Array<TrackerConnection>>(arrayOf())
    private val _errorMessage = MutableLiveData<String>()
    private val _newConnectionResult = MutableLiveData<ConnectionResult?>()

    val currentConnections: LiveData<Array<TrackerConnection>> = _currentConnections
    val errorMessage: LiveData<String> = _errorMessage
    val newConnectionResult: LiveData<ConnectionResult?> = _newConnectionResult

    fun fetchCurrentConnections() {
        activitySourcesService.currentConnections.enqueue { call, result ->
            if (result.isError) {
                _currentConnections.postValue(arrayOf())
                _errorMessage.postValue(result.error?.message)
                return@enqueue
            }
            _currentConnections.postValue(result.value)
        }
    }

    fun connect(activitySource: String) {
        activitySourcesService.connect(activitySource).enqueue { call, result ->
            if (result.isError) {
                _errorMessage.postValue(result.error!!.message)
                return@enqueue
            }
            _newConnectionResult.postValue(result.value)
            return@enqueue
        }
    }

    fun disconnect() {
        val connections = currentConnections.value
        if (connections.isNullOrEmpty()) {
            _errorMessage.postValue("No tracker connections")
            return
        }
        // NOTE: currently only one connection can be active
        val connection = connections.first()
        activitySourcesService.disconnect(connection).enqueue { call, result ->
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@enqueue
            }
            fetchCurrentConnections()
        }
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }
}

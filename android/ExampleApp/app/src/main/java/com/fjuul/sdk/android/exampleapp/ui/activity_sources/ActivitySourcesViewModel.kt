package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySource
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.TrackerConnection
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder

class ActivitySourcesViewModel() : ViewModel() {
    private val activitySourcesService = ActivitySourcesService(ApiClientHolder.sdkClient)
    private val _currentConnections = MutableLiveData<Array<TrackerConnection>>(arrayOf())
    private val _errorMessage = MutableLiveData<String>()
    private val _connectionIntent = MutableLiveData<Pair<ActivitySource, Intent>>()

    val currentConnections: LiveData<Array<TrackerConnection>> = _currentConnections
    val errorMessage: LiveData<String> = _errorMessage
    val connectionIntent: LiveData<Pair<ActivitySource, Intent>?> = _connectionIntent

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

    fun connect(activitySource: ActivitySource) {
        val manager = ActivitySourcesManager.getInstance()
        manager.connect(activitySource) { result ->
            if (result.isError) {
                _errorMessage.postValue(result.error!!.message)
                return@connect
            }
            _connectionIntent.postValue(Pair(activitySource, result.value!!))
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

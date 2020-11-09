package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySource
import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager

class ActivitySourcesViewModel() : ViewModel() {
    private val _currentConnections = MutableLiveData<List<ActivitySourceConnection>>(listOf())
    private val _errorMessage = MutableLiveData<String>()
    private val _connectionIntent = MutableLiveData<Pair<ActivitySource, Intent>>()

    val currentConnections: LiveData<List<ActivitySourceConnection>> = _currentConnections
    val errorMessage: LiveData<String> = _errorMessage
    val connectionIntent: LiveData<Pair<ActivitySource, Intent>?> = _connectionIntent

    fun fetchCurrentConnections() {
        val manager = ActivitySourcesManager.getInstance()
        manager.refreshCurrent { result ->
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@refreshCurrent
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
        val manager = ActivitySourcesManager.getInstance()
        manager.disconnect(connection) { result ->
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@disconnect
            }
            _currentConnections.postValue(result.value)
        }
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }
}

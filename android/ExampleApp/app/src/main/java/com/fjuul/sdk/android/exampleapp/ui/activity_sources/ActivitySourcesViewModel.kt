package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySource
import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager

class ActivitySourcesViewModel : ViewModel() {
    private val _currentConnections = MutableLiveData<List<ActivitySourceConnection>>(listOf())
    private val _errorMessage = MutableLiveData<String?>()
    private val _connectionIntent = MutableLiveData<Pair<ActivitySource, Intent>>()

    val currentConnections: LiveData<List<ActivitySourceConnection>> = _currentConnections
    val errorMessage: LiveData<String?> = _errorMessage
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
        manager.connect(activitySource) lit@{ result ->
            if (result.isError) {
                _errorMessage.postValue(result.error!!.message)
                return@lit
            }
            _connectionIntent.postValue(Pair(activitySource, result.value!!))
        }
    }

    fun isConnected(activitySource: ActivitySource): Boolean {
        val manager = ActivitySourcesManager.getInstance()
        return manager.current.find { isMatchedConnectionWithActivitySource(it, activitySource) } != null
    }

    fun disconnect() {
        val connections = currentConnections.value
        if (connections.isNullOrEmpty()) {
            _errorMessage.value = "No tracker connections"
            return
        }
        // NOTE: currently only one connection can be active
        val connection = connections.first()
        val manager = ActivitySourcesManager.getInstance()
        manager.disconnect(connection) lit@{ result ->
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@lit
            }
            _currentConnections.postValue(manager.current)
        }
    }

    fun disconnect(activitySource: ActivitySource) {
        val manager = ActivitySourcesManager.getInstance()
        val sourceConnection = manager.current.find { isMatchedConnectionWithActivitySource(it, activitySource) }
        if (sourceConnection == null) {
            _errorMessage.value = "No appropriate source connection to disconnect"
            return
        }
        manager.disconnect(sourceConnection) lit@{ result ->
            if (result.isError) {
                _errorMessage.postValue(result.error?.message)
                return@lit
            }
            _currentConnections.postValue(manager.current)
        }
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    fun postErrorMessage(errorMessage: String) {
        _errorMessage.postValue(errorMessage)
    }

    private fun isMatchedConnectionWithActivitySource(connection: ActivitySourceConnection, source: ActivitySource): Boolean {
        return connection.activitySource::class.java == source::class.java
    }
}

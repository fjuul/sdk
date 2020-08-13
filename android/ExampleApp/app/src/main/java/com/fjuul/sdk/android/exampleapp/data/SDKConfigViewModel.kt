package com.fjuul.sdk.android.exampleapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.entities.UserCredentials

class SDKConfigViewModel(private val appStorage: AppStorage) : ViewModel() {
    private val _apiKey = MutableLiveData<String>(appStorage.apiKey)
    val apiKey: LiveData<String> = _apiKey

    fun setApiKey(apiKey: String) {
        _apiKey.value = apiKey
        appStorage.apiKey = apiKey
    }

    private val _environment = MutableLiveData<SdkEnvironment>(appStorage.environment)
    val environment: LiveData<SdkEnvironment> = _environment

    fun setEnvironment(environment: SdkEnvironment) {
        _environment.value = environment
        appStorage.environment = environment
    }

    fun sdkConfig(): LiveData<Pair<String?, SdkEnvironment?>> {
        val liveDataMerger = MediatorLiveData<Pair<String?, SdkEnvironment?>>()
        liveDataMerger.addSource(apiKey) {
             liveDataMerger.value = (liveDataMerger.value ?: Pair(null, null)).copy(first = it)
        }
        liveDataMerger.addSource(environment) {
            liveDataMerger.value = (liveDataMerger.value ?: Pair(null, null)).copy(second = it)
        }
        return liveDataMerger
    }

    private val _userToken = MutableLiveData<String>(appStorage.userToken)
    val userToken: LiveData<String> = _userToken

    private val _userSecret = MutableLiveData<String>(appStorage.userSecret)
    val userSecret: LiveData<String> = _userSecret

    fun setUserCredentials(credentials: UserCredentials) {
        _userToken.value = credentials.token
        appStorage.userToken = credentials.token
        _userSecret.value = credentials.secret
        appStorage.userSecret = credentials.secret
    }
}

class SDKConfigViewModelFactory(private val appStorage: AppStorage): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = SDKConfigViewModel(appStorage) as T
}

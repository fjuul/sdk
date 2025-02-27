package com.fjuul.sdk.android.exampleapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.core.entities.UserCredentials

data class SdkUserConfigState(
    val environment: SdkEnvironment?,
    val apiKey: String?,
    val token: String?,
    val secret: String?
)

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
        liveDataMerger.value = Pair(apiKey.value, environment.value)
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

    fun setUserToken(token: String) {
        _userToken.value = token
        appStorage.userToken = token
    }

    private val _userSecret = MutableLiveData<String>(appStorage.userSecret)
    val userSecret: LiveData<String> = _userSecret

    fun setUserSecret(secret: String) {
        _userSecret.value = secret
        appStorage.userSecret = secret
    }

    fun postUserCredentials(credentials: UserCredentials) {
        _userToken.postValue(credentials.token)
        appStorage.userToken = credentials.token
        _userSecret.postValue(credentials.secret)
        appStorage.userSecret = credentials.secret
    }

    fun sdkUserConfigState(): LiveData<SdkUserConfigState> {
        val liveDataMerger = MediatorLiveData<SdkUserConfigState>()
        liveDataMerger.value = SdkUserConfigState(environment.value, apiKey.value, userToken.value, userSecret.value)
        liveDataMerger.addSource(apiKey) {
            liveDataMerger.value = liveDataMerger.value!!.copy(apiKey = it)
        }
        liveDataMerger.addSource(environment) {
            liveDataMerger.value = liveDataMerger.value!!.copy(environment = it)
        }
        liveDataMerger.addSource(userToken) {
            liveDataMerger.value = liveDataMerger.value!!.copy(token = it)
        }
        liveDataMerger.addSource(userSecret) {
            liveDataMerger.value = liveDataMerger.value!!.copy(secret = it)
        }
        return liveDataMerger
    }
}

class SDKConfigViewModelFactory(private val appStorage: AppStorage) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SDKConfigViewModel(appStorage) as T
}

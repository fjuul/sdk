package com.fjuul.sdk.android.exampleapp.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.user.entities.UserProfile
import com.fjuul.sdk.user.http.services.UserService
import java.util.concurrent.Executors

class AuthorizedUserDataViewModel : ViewModel() {
    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile
    private val _loginError = MutableLiveData<LoginError?>()
    val loginError: LiveData<LoginError?> = _loginError
    private val _activitySourcesInitialization = MutableLiveData<InitializationResult?>()
    val activitySourcesInitialization: LiveData<InitializationResult?> = _activitySourcesInitialization
    private val _logoutResult = MutableLiveData<LogoutResult?>()
    val logoutResult: LiveData<LogoutResult?> = _logoutResult
    private val _logoutInProgress = MutableLiveData(false)
    val logoutInProgress: LiveData<Boolean> = _logoutInProgress
    private var initializationRequestId = 0L
    private var logoutRequestId = 0L
    private var pendingLogout: PendingLogout? = null

    data class LoginError(val message: String, val destinationId: String)
    data class InitializationResult(val result: com.fjuul.sdk.core.entities.Result<Void>, val destinationId: String)
    data class LogoutResult(val backgroundWorkError: Throwable?, val storageRemoved: Boolean, val destinationId: String)
    private data class PendingLogout(val client: ApiClient, val context: Context, val destinationId: String)

    fun setUserProfile(profile: UserProfile) {
        _profile.value = profile
    }

    fun initializeActivitySources(client: ApiClient, config: ActivitySourcesManagerConfig, destinationId: String) {
        if (_logoutInProgress.value == true) {
            setLoginError("Please wait for logout to finish before signing in", destinationId)
            return
        }
        val requestId = ++initializationRequestId
        _activitySourcesInitialization.value = null
        _loginError.value = null
        ActivitySourcesManager.initializeAsync(client, config) { result ->
            if (!result.isError) {
                ApiClientHolder.setup(client)
            }
            if (requestId == initializationRequestId) {
                _activitySourcesInitialization.value = InitializationResult(result, destinationId)
            }
        }
    }

    fun consumeActivitySourcesInitialization() {
        _activitySourcesInitialization.value = null
    }

    fun setLoginError(message: String, destinationId: String) {
        _loginError.value = LoginError(message, destinationId)
    }

    fun consumeLoginError() {
        _loginError.value = null
    }

    fun logout(client: ApiClient, context: Context, destinationId: String) {
        if (_logoutInProgress.value == true) {
            return
        }
        val requestId = ++logoutRequestId
        _logoutResult.value = null
        _logoutInProgress.value = true
        pendingLogout = PendingLogout(client, context.applicationContext, destinationId)
        runLogoutCleanup(requestId)
    }

    fun retryLogoutCleanup() {
        val request = pendingLogout ?: return
        runLogoutCleanup(logoutRequestId, request)
    }

    private fun runLogoutCleanup(
        requestId: Long,
        request: PendingLogout? = pendingLogout
    ) {
        val pendingRequest = request ?: return
        ActivitySourcesManager.disableBackgroundWorkersAsync(pendingRequest.context) { cancellationResult ->
            if (cancellationResult.isError) {
                MAIN_HANDLER.post {
                    if (requestId == logoutRequestId) {
                        _logoutResult.value =
                            LogoutResult(cancellationResult.error, false, pendingRequest.destinationId)
                    }
                }
                return@disableBackgroundWorkersAsync
            }
            LOGOUT_STORAGE_EXECUTOR.execute {
                val storageRemoved = try {
                    pendingRequest.client.clearPersistentStorage()
                } catch (_: Throwable) {
                    false
                }
                MAIN_HANDLER.post {
                    if (requestId == logoutRequestId) {
                        if (storageRemoved) {
                            pendingLogout = null
                            _logoutInProgress.value = false
                        }
                        _logoutResult.value =
                            LogoutResult(null, storageRemoved, pendingRequest.destinationId)
                    }
                }
            }
        }
    }

    fun consumeLogoutResult() {
        _logoutResult.value = null
    }

    companion object {
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
        private val LOGOUT_STORAGE_EXECUTOR = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ExampleApp-Logout-Storage").apply { isDaemon = true }
        }
    }

    fun fetchUserProfile(client: ApiClient, callback: (success: Boolean, exception: Exception?) -> Unit) {
        UserService(client).profile.enqueue { _, result ->
            if (result.isError) {
                callback(false, result.error)
                return@enqueue
            }
            _profile.value = result.value!!
            callback(true, null)
        }
    }
}

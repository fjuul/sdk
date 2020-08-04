package com.fjuul.sdk.android.exampleapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.android.exampleapp.data.LoginRepository
import com.fjuul.sdk.android.exampleapp.data.Result

import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment

class OnboardingViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<OnboardingFormState>()
    val onboardingFormState: LiveData<OnboardingFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        val result = loginRepository.login(username, password)

        if (result is Result.Success) {
            _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
        } else {
            _loginResult.value = LoginResult(error = R.string.login_failed)
        }
    }

    fun loginDataChanged(apiKey: String, token: String, secret: String) {
        when {
            apiKey.isEmpty() -> {
                _loginForm.value = OnboardingFormState(apiKeyError = R.string.invalid_api_key);
            }
            token.isEmpty() -> {
                _loginForm.value = OnboardingFormState(tokenError = R.string.invalid_token)
            }
            secret.isEmpty() -> {
                _loginForm.value = OnboardingFormState(secretError = R.string.invalid_secret)
            }
            else -> {
                _loginForm.value = OnboardingFormState(isDataValid = true)
            }
        }
    }

    fun envModeChanged(environment: SdkEnvironment) {
        if (_loginForm.value != null) {
            _loginForm.value = _loginForm.value!!.copy(environment = environment)
        } else {
            _loginForm.value = OnboardingFormState(environment = environment)
        }
    }
}

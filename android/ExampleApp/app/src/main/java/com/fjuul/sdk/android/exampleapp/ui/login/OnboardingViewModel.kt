package com.fjuul.sdk.android.exampleapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.android.exampleapp.data.LoginRepository

import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment

class OnboardingViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<OnboardingFormState>(OnboardingFormState())
    val onboardingFormState: LiveData<OnboardingFormState> = _loginForm

    private val _submittedForm = MutableLiveData<OnboardingFormState>()
    val submittedFormState: LiveData<OnboardingFormState> = _submittedForm

    fun submit() {
        _submittedForm.value = _loginForm.value
    }

    fun loginDataChanged(apiKey: String, token: String, secret: String) {
        when {
            apiKey.isEmpty() -> {
                _loginForm.value = _loginForm.value!!.copy(apiKeyError = R.string.invalid_api_key);
            }
            token.isEmpty() -> {
                _loginForm.value = _loginForm.value!!.copy(tokenError = R.string.invalid_token)
            }
            secret.isEmpty() -> {
                _loginForm.value = _loginForm.value!!.copy(secretError = R.string.invalid_secret)
            }
            else -> {
                _loginForm.value = _loginForm.value!!.copy(isDataValid = true)
            }
        }
    }

    fun envModeChanged(environment: SdkEnvironment) {
        _loginForm.value = _loginForm.value!!.copy(environment = environment)
    }
}

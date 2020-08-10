package com.fjuul.sdk.android.exampleapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.LoginRepository
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
        var loginForm = _loginForm.value!!.copy(apiKeyError = null, tokenError = null, secretError = null, isDataValid = false)
        if (apiKey.isEmpty()) {
            loginForm = loginForm.copy(apiKeyError = R.string.invalid_api_key)
        }
        if (token.isEmpty()) {
            loginForm = loginForm.copy(tokenError = R.string.invalid_token)
        }
        if (secret.isEmpty()) {
            loginForm = loginForm.copy(secretError = R.string.invalid_secret)
        }
        if (loginForm.apiKeyError == null && loginForm.tokenError == null && loginForm.secretError == null) {
            loginForm = loginForm.copy(isDataValid = true)
        }
        _loginForm.value = loginForm
    }

    fun envModeChanged(environment: SdkEnvironment) {
        _loginForm.value = _loginForm.value!!.copy(environment = environment)
    }
}

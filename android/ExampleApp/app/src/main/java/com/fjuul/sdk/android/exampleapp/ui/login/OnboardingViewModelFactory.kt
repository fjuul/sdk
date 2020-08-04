package com.fjuul.sdk.android.exampleapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.data.LoginDataSource
import com.fjuul.sdk.android.exampleapp.data.LoginRepository

/**
 * ViewModel provider factory to instantiate OnboardingViewModel.
 * Required given OnboardingViewModel has a non-empty constructor
 */
class OnboardingViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            return OnboardingViewModel(
                    loginRepository = LoginRepository(
                            dataSource = LoginDataSource()
                    )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

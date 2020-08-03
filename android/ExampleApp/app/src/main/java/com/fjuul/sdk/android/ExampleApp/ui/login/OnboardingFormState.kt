package com.fjuul.sdk.android.exampleapp.ui.login

/**
 * Data validation state of the login form.
 */
data class OnboardingFormState(val usernameError: Int? = null,
                               val passwordError: Int? = null,
                               val isDataValid: Boolean = false)

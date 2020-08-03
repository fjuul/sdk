package com.fjuul.sdk.android.exampleapp.ui.login

/**
 * Data validation state of the login form.
 */
data class OnboardingFormState(val tokenError: Int? = null,
                               val secretError: Int? = null,
                               val apiKeyError: Int? = null,
                               val isDataValid: Boolean = false)

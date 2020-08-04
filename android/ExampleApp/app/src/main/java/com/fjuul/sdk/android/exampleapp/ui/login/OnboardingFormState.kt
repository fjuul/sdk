package com.fjuul.sdk.android.exampleapp.ui.login

/**
 * Data validation state of the login form.
 */

enum class SdkEnvironment {
    DEV, TEST, PROD
}

data class OnboardingFormState(val tokenError: Int? = null,
                               val secretError: Int? = null,
                               val apiKeyError: Int? = null,
                               val environment: SdkEnvironment? = null,
                               val isDataValid: Boolean = false)

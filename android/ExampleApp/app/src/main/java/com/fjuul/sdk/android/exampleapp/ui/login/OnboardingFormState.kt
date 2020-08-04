package com.fjuul.sdk.android.exampleapp.ui.login

import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment

/**
 * Data validation state of the login form.
 */

data class OnboardingFormState(val tokenError: Int? = null,
                               val secretError: Int? = null,
                               val apiKeyError: Int? = null,
                               val environment: SdkEnvironment? = null,
                               val isDataValid: Boolean = false)

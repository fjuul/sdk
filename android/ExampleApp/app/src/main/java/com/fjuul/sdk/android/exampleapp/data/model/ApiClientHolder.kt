package com.fjuul.sdk.android.exampleapp.data.model
import android.content.Context
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment
import com.fjuul.sdk.entities.UserCredentials
import com.fjuul.sdk.http.ApiClient

object ApiClientHolder {
    private var sdkClient: ApiClient? = null
        get() {
            return sdkClient
        }

    fun setup(context: Context, env: SdkEnvironment, apiKey: String, token: String, secret: String) {
        sdkClient = ApiClient.Builder(context, getBaseUrlByEnv(env), apiKey)
            .setUserCredentials(UserCredentials(token, secret))
            .build()
    }

    fun setup(context: Context, env: SdkEnvironment, apiKey: String) {
        sdkClient = ApiClient.Builder(context, getBaseUrlByEnv(env), apiKey).build()
    }

    private fun getBaseUrlByEnv(env: SdkEnvironment): String {
        return when (env) {
            SdkEnvironment.DEV -> "https://dev.api.fjuul.com"
            SdkEnvironment.TEST -> "https://test.api.fjuul.com"
            SdkEnvironment.PROD -> "https://api.fjuul.com"
        }
    }
}

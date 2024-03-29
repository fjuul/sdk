package com.fjuul.sdk.android.exampleapp.data.model
import android.content.Context
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment
import com.fjuul.sdk.core.ApiClient

object ApiClientHolder {
    lateinit var sdkClient: ApiClient
        private set

    fun setup(apiClient: ApiClient) {
        sdkClient = apiClient
    }

    fun setup(context: Context, env: SdkEnvironment, apiKey: String) {
        sdkClient = ApiClient.Builder(context, getBaseUrlByEnv(env), apiKey).build()
    }

    fun getBaseUrlByEnv(env: SdkEnvironment): String {
        return when (env) {
            SdkEnvironment.DEV -> "https://dev.api.fjuul.com"
            SdkEnvironment.TEST -> "https://test.api.fjuul.com"
            SdkEnvironment.PROD -> "https://api.fjuul.com"
        }
    }
}

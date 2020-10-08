package com.fjuul.sdk.android.exampleapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppStorage(private val context: Context) {
    enum class Key(val value: String) {
        USER_TOKEN("USER_TOKEN"),
        USER_SECRET("USER_SECRET"),
        API_KEY("API_KEY"),
        ENVIRONMENT("ENVIRONMENT")
    }

    private val sharedPreferences = context.getSharedPreferences("APP_STORE", Context.MODE_PRIVATE)

    var userToken: String?
        get() = sharedPreferences.getString(Key.USER_TOKEN)
        set(value) = putStringAndCommit(Key.USER_TOKEN, value)

    var userSecret: String?
        get() = sharedPreferences.getString(Key.USER_SECRET)
        set(value) = putStringAndCommit(Key.USER_SECRET, value)

    var apiKey: String?
        get() = sharedPreferences.getString(Key.API_KEY)
        set(value) = putStringAndCommit(Key.API_KEY, value)

    var environment: SdkEnvironment?
        get() {
            val envRaw = sharedPreferences.getString(Key.ENVIRONMENT)
            if (envRaw != null) {
                return SdkEnvironment.valueOf(envRaw)
            }
            return null
        }
        set(value) = putStringAndCommit(Key.ENVIRONMENT, value?.name)

    fun reset() {
        sharedPreferences.edit(true) { reset() }
    }

    private fun putStringAndCommit(key: Key, value: String?) {
        sharedPreferences.edit(true) {
            putString(key.value, value)
        }
    }

    private fun SharedPreferences.getString(key: Key): String? {
        return getString(key.value, null)
    }
}

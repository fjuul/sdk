package com.fjuul.sdk.android.exampleapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.user.entities.UserProfile
import com.fjuul.sdk.user.http.services.UserService

class AuthorizedUserDataViewModel : ViewModel() {
    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    fun setUserProfile(profile: UserProfile) {
        _profile.value = profile
    }

    fun fetchUserProfile(client: ApiClient, callback: (success: Boolean, exception: Exception?) -> Unit) {
        UserService(client).profile.enqueue { _, result ->
            if (result.isError) {
                callback(false, result.error)
                return@enqueue
            }
            _profile.value = result.value!!
            callback(true, null)
        }
    }
}

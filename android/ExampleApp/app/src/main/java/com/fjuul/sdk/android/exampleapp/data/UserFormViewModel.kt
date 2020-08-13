package com.fjuul.sdk.android.exampleapp.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import com.fjuul.sdk.http.utils.ApiCall
import com.fjuul.sdk.user.entities.Gender
import com.fjuul.sdk.user.entities.UserCreationResult
import com.fjuul.sdk.user.entities.UserProfile
import com.fjuul.sdk.user.http.services.UserService
import java.lang.Error
import java.time.LocalDate

class UserFormViewModel: ViewModel() {
    private var _profileBuilder: UserProfile.PartialBuilder? = null
    private val profileBuilder: UserProfile.PartialBuilder
        private get() {
            if (_profileBuilder == null) {
                _profileBuilder = UserProfile.PartialBuilder()
            }
            return _profileBuilder!!
        }

    private val _birthDate = MutableLiveData<LocalDate>(LocalDate.of(1980, 10, 20))
    val birthDate: LiveData<LocalDate> = _birthDate

    fun setBirthDate(date: LocalDate) {
        _birthDate.value = date
        profileBuilder?.setBirthDate(date)
    }

    private val _height = MutableLiveData<Float>()
    val height: MutableLiveData<Float> = _height

    fun setHeight(height: Float?) {
        _height.value = height
        profileBuilder?.setHeight(height ?: 0f)
    }

    private val _weight = MutableLiveData<Float>()
    val weight: MutableLiveData<Float> = _weight

    fun setWeight(weight: Float?) {
        _weight.value = weight
        profileBuilder?.setWeight(weight ?: 0f)
    }

    private val _gender = MutableLiveData<Gender>()
    private val gender: LiveData<Gender> = _gender

    fun setGender(gender: Gender) {
        _gender.value = gender
        profileBuilder?.setGender(gender)
    }

    @Throws(Error::class)
    fun createUser(context: Context, apiKey: String, sdkEnvironment: SdkEnvironment): ApiCall<UserCreationResult> {
        val partialProfile = _profileBuilder ?: throw Error("empty profile params")
        ApiClientHolder.setup(context, sdkEnvironment, apiKey)
        return UserService(ApiClientHolder.sdkClient).createUser(partialProfile)
    }

    //TODO: add method to fill live data by user-profile
}

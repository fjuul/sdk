package com.fjuul.sdk.android.exampleapp.ui.user_profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.AppStorage
import com.fjuul.sdk.android.exampleapp.data.AuthorizedUserDataViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModelFactory
import com.fjuul.sdk.android.exampleapp.data.UserFormViewModel
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import com.fjuul.sdk.android.exampleapp.ui.login.afterTextChanged
import com.fjuul.sdk.core.entities.UserCredentials
import com.fjuul.sdk.core.exceptions.ApiExceptions
import com.fjuul.sdk.user.entities.Gender
import com.fjuul.sdk.user.entities.UserProfile
import com.fjuul.sdk.user.exceptions.UserApiExceptions.ValidationErrorBadRequestException
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

class UserProfileFragment : Fragment() {
    private val args: UserProfileFragmentArgs by navArgs()
    private val model: UserFormViewModel by viewModels()
    private val sdkConfigViewModel: SDKConfigViewModel by activityViewModels {
        SDKConfigViewModelFactory(AppStorage(requireContext()))
    }
    private val authorizedUserDataViewModel: AuthorizedUserDataViewModel by activityViewModels()
    private var wasPrefilled = false

    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var birthdateInput: View
    private lateinit var birthdateText: TextView
    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button
    private lateinit var deleteButton: Button
    private lateinit var timezoneTextField: TextInputLayout
    private lateinit var localeTextField: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_popup_item,
            GENDERS
        )
        genderDropdown = view.findViewById(R.id.gender_filled_exposed_dropdown)
        genderDropdown.keyListener = null
        genderDropdown.setAdapter(adapter)

        birthdateInput = view.findViewById(R.id.birthdate_block)
        birthdateText = view.findViewById(R.id.birthdate_input)
        heightInput = view.findViewById(R.id.height_input)
        weightInput = view.findViewById(R.id.weight_input)
        submitButton = view.findViewById(R.id.create_user_submit_button)
        deleteButton = view.findViewById(R.id.delete_user_submit_button)
        timezoneTextField = view.findViewById(R.id.timezoneTextField)
        localeTextField = view.findViewById(R.id.localeTextField)

        if (args.flow == UserProfileNavigationFlow.UPDATE) {
            timezoneTextField.visibility = View.VISIBLE
            localeTextField.visibility = View.VISIBLE
            submitButton.text = "UPDATE"
            deleteButton.text = "DELETE PROFILE"

            model.profileWasRefreshed.observe(
                viewLifecycleOwner,
                Observer { refreshed ->
                    if (refreshed) { return@Observer }
                    model.profileWasRefreshed.value = true
                    authorizedUserDataViewModel.fetchUserProfile(ApiClientHolder.sdkClient) { _, exception ->
                        if (exception != null) {
                            AlertDialog.Builder(requireContext()).setMessage(exception.message).show()
                            return@fetchUserProfile
                        }
                        val profile = authorizedUserDataViewModel.profile.value
                        if (profile != null) {
                            prefillWithUserProfile(profile)
                        }
                    }
                }
            )

            val profile = authorizedUserDataViewModel.profile.value
            if (!wasPrefilled && profile != null) {
                wasPrefilled = true
                prefillWithUserProfile(profile)
            }
        }

        // birthdate
        model.birthDate.observe(
            viewLifecycleOwner,
            Observer { date ->
                birthdateText.text = date?.toString()
            }
        )
        birthdateInput.setOnClickListener {
            val date = model.birthDate?.value ?: LocalDate.of(1980, 10, 20)
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    model.setBirthDate(LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        // height
        heightInput.afterTextChanged {
            model.setHeight(it.toFloatOrNull())
        }

        // weight
        weightInput.afterTextChanged {
            model.setWeight(it.toFloatOrNull())
        }

        // gender
        genderDropdown.afterTextChanged {
            when (it) {
                GenderPicker.MALE.displayName -> model.setGender(Gender.male)
                GenderPicker.FEMALE.displayName -> model.setGender(Gender.female)
                GenderPicker.OTHER.displayName -> model.setGender(Gender.other)
            }
        }

        // timezone
        timezoneTextField.editText?.afterTextChanged {
            model.setTimezone(it)
        }

        localeTextField.editText?.afterTextChanged {
            model.setLocale(it)
        }

        submitButton.setOnClickListener {
            if (args.flow == UserProfileNavigationFlow.CREATE) {
                try {
                    val (token, env) = sdkConfigViewModel.sdkConfig().value!!
                    model.createUser(requireContext(), token!!, env!!).enqueue { _, result ->
                        if (result.isError) {
                            buildAlertFromApiException(result.error!!).show()
                            return@enqueue
                        }
                        val creationResult = result.value!!
                        val token = creationResult.user.token
                        sdkConfigViewModel.postUserCredentials(UserCredentials(token, creationResult.secret))
                        val action = UserProfileFragmentDirections.actionCreateUserFragmentToLoginFragment()
                        findNavController().navigate(action)
                    }
                } catch (exception: Exception) {
                    AlertDialog.Builder(requireContext()).setMessage(exception.message).show()
                }
            } else if (args.flow == UserProfileNavigationFlow.UPDATE) {
                try {
                    model.updateUser(ApiClientHolder.sdkClient).enqueue { call, result ->
                        if (result.isError) {
                            buildAlertFromApiException(result.error!!).show()
                            return@enqueue
                        }
                        val profile = result.value!!
                        authorizedUserDataViewModel.setUserProfile(profile)
                        prefillWithUserProfile(profile)
                    }
                } catch (exception: Exception) {
                    AlertDialog.Builder(requireContext()).setMessage(exception.message).show()
                }
            }
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure you want to mark your profile for deletion?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.setPositiveButton("Delete") { dialog, _ ->
                    try {
                        model.deleteUser(ApiClientHolder.sdkClient).enqueue { call, result ->
                            if (result.isError) {
                                buildAlertFromApiException(result.error!!).show()
                                return@enqueue
                            }
                        }
                    } catch (exception: Exception) {
                        AlertDialog.Builder(requireContext()).setMessage(exception.message).show()
                    }
                    dialog.dismiss()
                    val action = UserProfileFragmentDirections.actionCreateUserFragmentToLoginFragment()
                    findNavController().navigate(action)
                }.show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            UserProfileFragment().apply {
                arguments = Bundle()
            }

        val GENDERS = arrayOf(
            GenderPicker.MALE.displayName,
            GenderPicker.FEMALE.displayName,
            GenderPicker.OTHER.displayName
        )
    }

    private fun prefillWithUserProfile(profile: UserProfile) {
        genderDropdown.setText(profile.gender.name.toUpperCase(), false)
        birthdateText.text = profile.birthDate.toString()
        heightInput.setText(profile.height.toString(), TextView.BufferType.NORMAL)
        weightInput.setText(profile.weight.toString(), TextView.BufferType.NORMAL)
        timezoneTextField.editText?.setText(profile.timezone.id, TextView.BufferType.NORMAL)
        localeTextField.editText?.setText(profile.locale, TextView.BufferType.NORMAL)
    }

    private fun buildAlertFromApiException(exception: ApiExceptions.CommonException): AlertDialog.Builder {
        val alertTitle = exception.message
        var alertMessage: String? = null
        if (exception is ValidationErrorBadRequestException) {
            alertMessage = exception.errors.joinToString("\n") {
                return@joinToString "${it.property}: ${it.constraints}"
            }
        }
        val alert = AlertDialog.Builder(requireContext()).setTitle(alertTitle)
        if (alertMessage != null) {
            alert.setMessage(alertMessage)
        }
        return alert
    }
}

public enum class GenderPicker(val displayName: String) {
    MALE("MALE"),
    FEMALE("FEMALE"),
    OTHER("OTHER")
}

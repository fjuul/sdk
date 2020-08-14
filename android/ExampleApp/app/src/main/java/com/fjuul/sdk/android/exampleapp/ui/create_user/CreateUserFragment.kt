package com.fjuul.sdk.android.exampleapp.ui.create_user

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
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModelFactory
import com.fjuul.sdk.android.exampleapp.data.UserFormViewModel
import com.fjuul.sdk.android.exampleapp.ui.login.LoginFragmentDirections
import com.fjuul.sdk.android.exampleapp.ui.login.afterTextChanged
import com.fjuul.sdk.entities.UserCredentials
import com.fjuul.sdk.user.entities.Gender
import java.time.LocalDate

class CreateUserFragment : Fragment() {
    private val model: UserFormViewModel by viewModels()
    private val sdkConfigViewModel: SDKConfigViewModel by activityViewModels {
        SDKConfigViewModelFactory(AppStorage(requireContext()))
    }

    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var birthdateInput: View
    private lateinit var birthdateText: TextView
    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_user, container, false)
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

        // birthdate
        model.birthDate.observe(viewLifecycleOwner, Observer { date ->
            birthdateText.text = date.toString()
        })
        birthdateInput.setOnClickListener {
            val date = model.birthDate.value!!
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

        submitButton.setOnClickListener {
            try {
                val (token, env) = sdkConfigViewModel.sdkConfig().value!!
                model.createUser(requireContext(), token!!, env!!).enqueue { _, result ->
                    if (result.isError) {
                        AlertDialog.Builder(requireContext()).setMessage(result.error?.message)
                            .show()
                        return@enqueue
                    }
                    val creationResult = result.value!!
                    val token = creationResult.user.token
                    sdkConfigViewModel.postUserCredentials(UserCredentials(token, creationResult.secret))
                    val action = CreateUserFragmentDirections.actionCreateUserFragmentToLoginFragment()
                    findNavController().navigate(action)
                }
            } catch (error: Error) {
                AlertDialog.Builder(requireContext()).setMessage(error.message).show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CreateUserFragment().apply {
                arguments = Bundle()
            }

        val GENDERS = arrayOf(
            GenderPicker.MALE.displayName,
            GenderPicker.FEMALE.displayName,
            GenderPicker.OTHER.displayName)
    }
}

public enum class GenderPicker(val displayName: String) {
    MALE("MALE"),
    FEMALE("FEMALE"),
    OTHER("OTHER")
}

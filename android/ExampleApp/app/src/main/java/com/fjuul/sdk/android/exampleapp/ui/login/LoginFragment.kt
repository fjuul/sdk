package com.fjuul.sdk.android.exampleapp.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.AppStorage
import com.fjuul.sdk.android.exampleapp.data.AuthorizedUserDataViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModelFactory
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder

class LoginFragment : Fragment() {
    private val sdkConfigViewModel: SDKConfigViewModel by activityViewModels {
        SDKConfigViewModelFactory(AppStorage(requireContext()))
    }
    private val authorizedUserDataViewModel: AuthorizedUserDataViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiKeyInput = view.findViewById<EditText>(R.id.api_key_input)
        val tokenInput = view.findViewById<EditText>(R.id.user_token)
        val secretInput = view.findViewById<EditText>(R.id.user_secret)
        val continueButton = view.findViewById<Button>(R.id.continue_btn)
        val createUserButton = view.findViewById<Button>(R.id.create_user_button)
        val radioGroup = view.findViewById<RadioGroup>(R.id.env_radio_group)

        // initial setup by view models
        apiKeyInput.setText(sdkConfigViewModel.apiKey.value, TextView.BufferType.NORMAL)
        when (sdkConfigViewModel.environment.value) {
            SdkEnvironment.DEV -> view.findViewById<RadioButton>(R.id.dev_env_radio).isChecked = true
            SdkEnvironment.TEST -> view.findViewById<RadioButton>(R.id.test_env_radio).isChecked = true
            SdkEnvironment.PROD -> view.findViewById<RadioButton>(R.id.prod_env_radio).isChecked = true
        }
        tokenInput.setText(sdkConfigViewModel.userToken.value)
        secretInput.setText(sdkConfigViewModel.userSecret.value)

        sdkConfigViewModel.sdkConfig().observe(
            viewLifecycleOwner,
            Observer {
                createUserButton.isEnabled = !it.first.isNullOrEmpty() && it.second != null
            }
        )
        sdkConfigViewModel.sdkUserConfigState().observe(
            viewLifecycleOwner,
            Observer {
                val (apiKey, env, token, secret) = it
                continueButton.isEnabled = apiKey != null && env != null && !token.isNullOrBlank() && !secret.isNullOrBlank()
            }
        )

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.dev_env_radio -> sdkConfigViewModel.setEnvironment(SdkEnvironment.DEV)
                R.id.test_env_radio -> sdkConfigViewModel.setEnvironment(SdkEnvironment.TEST)
                R.id.prod_env_radio -> sdkConfigViewModel.setEnvironment(SdkEnvironment.PROD)
            }
        }

        apiKeyInput.afterTextChanged {
            sdkConfigViewModel.setApiKey(it)
        }

        createUserButton.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToCreateUserFragment()
            findNavController().navigate(action)
        }

        tokenInput.afterTextChanged {
            sdkConfigViewModel.setUserToken(it)
        }

        secretInput.apply {
            afterTextChanged {
                sdkConfigViewModel.setUserSecret(it)
            }

            continueButton.setOnClickListener {
                val (env, apiKey, token, secret) = sdkConfigViewModel.sdkUserConfigState().value!!
                ApiClientHolder.setup(
                    context = requireContext(),
                    env = env!!,
                    apiKey = apiKey!!,
                    token = token!!,
                    secret = secret!!
                )
                authorizedUserDataViewModel.fetchUserProfile(ApiClientHolder.sdkClient) { success, exception ->
                    if (success) {
                        val action = LoginFragmentDirections.actionLoginFragmentToModulesFragment()
                        findNavController().navigate(action)
                    } else {
                        AlertDialog.Builder(requireContext()).setMessage(exception?.message ?: "Unknown Error").show()
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = LoginFragment().apply {
            arguments = Bundle()
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

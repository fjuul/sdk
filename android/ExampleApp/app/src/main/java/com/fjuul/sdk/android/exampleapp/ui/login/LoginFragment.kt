package com.fjuul.sdk.android.exampleapp.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder

class LoginFragment : Fragment() {
    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
        val login = view.findViewById<Button>(R.id.continue_btn)
        val createUserButton = view.findViewById<Button>(R.id.create_user_button)
        val radioGroup = view.findViewById<RadioGroup>(R.id.env_radio_group)


        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.dev_env_radio -> onboardingViewModel.envModeChanged(SdkEnvironment.DEV)
                R.id.test_env_radio -> onboardingViewModel.envModeChanged(SdkEnvironment.TEST)
                R.id.prod_env_radio -> onboardingViewModel.envModeChanged(SdkEnvironment.PROD)
            }
        }

        onboardingViewModel = ViewModelProviders.of(this, OnboardingViewModelFactory())
            .get(OnboardingViewModel::class.java)

        onboardingViewModel.onboardingFormState.observe(viewLifecycleOwner, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid && loginState.environment != null

            if (loginState.apiKeyError != null) {
                apiKeyInput.error = getString(loginState.apiKeyError)
            }
            if (loginState.tokenError != null) {
                tokenInput.error = getString(loginState.tokenError)
            }
            if (loginState.secretError != null) {
                secretInput.error = getString(loginState.secretError)
            }
        })

        onboardingViewModel.submittedFormState.observe(viewLifecycleOwner, Observer {
            val loginResult = it ?: return@Observer

            ApiClientHolder.setup(
                context = this.requireActivity().applicationContext,
                env = it.environment!!,
                apiKey = apiKeyInput.text.toString(),
                token = tokenInput.text.toString(),
                secret = secretInput.text.toString()
            )

            val action = LoginFragmentDirections.actionLoginFragmentToModulesFragment()
            findNavController().navigate(action)
        })

        apiKeyInput.afterTextChanged {
            createUserButton.isEnabled = it.isNotEmpty()
            onboardingViewModel.loginDataChanged(
                apiKeyInput.text.toString(),
                tokenInput.text.toString(),
                secretInput.text.toString()
            )
        }

        tokenInput.afterTextChanged {
            onboardingViewModel.loginDataChanged(
                apiKeyInput.text.toString(),
                tokenInput.text.toString(),
                secretInput.text.toString()
            )
        }

        secretInput.apply {
            afterTextChanged {
                onboardingViewModel.loginDataChanged(
                    apiKeyInput.text.toString(),
                    tokenInput.text.toString(),
                    secretInput.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        onboardingViewModel.submit()
                }
                false
            }

            login.setOnClickListener {
                onboardingViewModel.submit()
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
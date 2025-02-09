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
import androidx.navigation.fragment.findNavController
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.AppStorage
import com.fjuul.sdk.android.exampleapp.data.AuthorizedUserDataViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModel
import com.fjuul.sdk.android.exampleapp.data.SDKConfigViewModelFactory
import com.fjuul.sdk.android.exampleapp.data.SdkEnvironment
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.UserCredentials
import java.time.Duration
import java.util.stream.Collectors
import java.util.stream.Stream

class LoginFragment : Fragment() {
    private val sdkConfigViewModel: SDKConfigViewModel by activityViewModels {
        SDKConfigViewModelFactory(AppStorage(requireContext()))
    }
    private val authorizedUserDataViewModel: AuthorizedUserDataViewModel by activityViewModels()

    private val activitySourcesManagerConfig: ActivitySourcesManagerConfig by lazy {
        val minSessionDuration = Duration.ofMinutes(5)
        val allFitnessMetrics = Stream.of(
            FitnessMetricsType.INTRADAY_CALORIES,
            FitnessMetricsType.INTRADAY_HEART_RATE,
            FitnessMetricsType.INTRADAY_STEPS,
            FitnessMetricsType.WORKOUTS,
            FitnessMetricsType.HEIGHT,
            FitnessMetricsType.WEIGHT
        )
            .collect(Collectors.toSet())
        return@lazy ActivitySourcesManagerConfig.Builder()
            .enableGoogleFitBackgroundSync(minSessionDuration)
            .enableProfileBackgroundSync()
            .setCollectableFitnessMetrics(allFitnessMetrics)
            .build()
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
            SdkEnvironment.DEV ->
                view.findViewById<RadioButton>(R.id.dev_env_radio).isChecked =
                    true
            SdkEnvironment.TEST ->
                view.findViewById<RadioButton>(R.id.test_env_radio).isChecked =
                    true
            SdkEnvironment.PROD ->
                view.findViewById<RadioButton>(R.id.prod_env_radio).isChecked =
                    true
            else -> {}
        }
        tokenInput.setText(sdkConfigViewModel.userToken.value)
        secretInput.setText(sdkConfigViewModel.userSecret.value)

        sdkConfigViewModel.sdkConfig().observe(
            viewLifecycleOwner
        ) {
            createUserButton.isEnabled = !it.first.isNullOrEmpty() && it.second != null
        }
        sdkConfigViewModel.sdkUserConfigState().observe(
            viewLifecycleOwner
        ) {
            val (apiKey, env, token, secret) = it
            continueButton.isEnabled =
                apiKey != null && env != null && !token.isNullOrBlank() && !secret.isNullOrBlank()
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
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
                val apiClient = ApiClient.Builder(
                    context,
                    ApiClientHolder.getBaseUrlByEnv(env!!), apiKey!!
                )
                    .setUserCredentials(UserCredentials(token!!, secret!!))
                    .build()
                authorizedUserDataViewModel.fetchUserProfile(apiClient) { success, exception ->
                    ApiClientHolder.setup(apiClient)
                    ActivitySourcesManager.initialize(apiClient, activitySourcesManagerConfig)
                    if (success) {
                        val action = LoginFragmentDirections.actionLoginFragmentToModulesFragment()
                        findNavController().navigate(action)
                    } else {
                        AlertDialog.Builder(requireContext()).setMessage(
                            exception?.message ?: "Unknown Error"
                        ).show()
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

package com.fjuul.sdk.android.exampleapp.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.fjuul.sdk.android.exampleapp.ui.modules.ModulesActivity

import com.fjuul.sdk.android.exampleapp.R

class LoginActivity : AppCompatActivity() {

    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val tokenInput = findViewById<EditText>(R.id.user_token)
        val secretInput = findViewById<EditText>(R.id.user_secret)
        val login = findViewById<Button>(R.id.continue_btn)
        val createUserButton = findViewById<Button>(R.id.create_user_button);



        onboardingViewModel = ViewModelProviders.of(this, OnboardingViewModelFactory())
                .get(OnboardingViewModel::class.java)

        onboardingViewModel.onboardingFormState.observe(this@LoginActivity, Observer {
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

        onboardingViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            val intent = Intent(this, ModulesActivity::class.java).apply {
                // putExtra...
            }
            startActivity(intent)
//            if (loginResult.error != null) {
//                showLoginFailed(loginResult.error)
//            }
//            if (loginResult.success != null) {
//                updateUiWithUser(loginResult.success)
//            }
            setResult(Activity.RESULT_OK)
//
//            //Complete and destroy login activity once successful
            finish()
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
                        onboardingViewModel.login(
                                apiKeyInput.text.toString(),
                                tokenInput.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                onboardingViewModel.login(apiKeyInput.text.toString(), tokenInput.text.toString())
            }
        }
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.dev_env_radio ->
                    if (checked) {
                        onboardingViewModel.envModeChanged(SdkEnvironment.DEV);
                    }
                R.id.test_env_radio ->
                    if (checked) {
                        onboardingViewModel.envModeChanged(SdkEnvironment.TEST);
                    }
                R.id.prod_env_radio ->
                    if (checked) {
                        onboardingViewModel.envModeChanged(SdkEnvironment.PROD);
                    }
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
                applicationContext,
                "$welcome $displayName",
                Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
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

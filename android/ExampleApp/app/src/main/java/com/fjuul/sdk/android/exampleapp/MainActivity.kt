package com.fjuul.sdk.android.exampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.fjuul.sdk.activitysources.entities.ExternalAuthenticationFlowHandler
import com.fjuul.sdk.android.exampleapp.databinding.ActivityMainBinding
import com.fjuul.sdk.android.exampleapp.ui.activity_sources.ActivitySourcesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)
        if (savedInstanceState == null) {
            handleConnectionCallback(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleConnectionCallback(intent)
    }

    private fun handleConnectionCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "fjuulsdk-exampleapp") return
        val status = ExternalAuthenticationFlowHandler.handle(uri) ?: return
        if (status.isSuccess) {
            val navigationController = binding.navHostFragment.findNavController()
            if (navigationController.currentDestination?.id == R.id.activitySourcesFragment) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHostFragment.childFragmentManager.fragments.filterIsInstance<ActivitySourcesFragment>().forEach {
                    it.refreshCurrentConnections()
                }
            }
            return
        }

        val (title, message) = when (status.errorCode) {
            ExternalAuthenticationFlowHandler.ErrorCode.OAUTH_CANCELLED ->
                "Connection Cancelled" to "The provider cancelled the connection. You can try again."
            ExternalAuthenticationFlowHandler.ErrorCode.GOOGLE_HEALTH_ACCOUNT_NOT_LINKED ->
                "Google Health Account Required" to
                    "Create a Google Health profile or migrate your Fitbit account, then return and retry the connection."
            else -> "Connection Failed" to "The tracker could not be connected. Please try again."
        }
        val alert = AlertDialog.Builder(this).setTitle(title).setMessage(message)
        if (status.errorCode == ExternalAuthenticationFlowHandler.ErrorCode.GOOGLE_HEALTH_ACCOUNT_NOT_LINKED) {
            alert.setPositiveButton("Account Setup") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fitbit.google.com/auth/signup")))
            }
            alert.setNegativeButton("OK", null)
        } else {
            alert.setPositiveButton("OK", null)
        }
        alert.show()
    }
}

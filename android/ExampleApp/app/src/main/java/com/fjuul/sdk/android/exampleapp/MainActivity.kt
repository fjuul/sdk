package com.fjuul.sdk.android.exampleapp

import android.content.Intent
import android.os.Bundle
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
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data?.scheme == "fjuulsdk-exampleapp") {
            val redirectResult = ExternalAuthenticationFlowHandler.handle(intent.data!!)
            if (redirectResult != null && redirectResult.isSuccess) {
                val navigationController = binding.navHostFragment.findNavController()
                if (navigationController.currentDestination?.id == R.id.activitySourcesFragment) {
                    supportFragmentManager.fragments.forEach {
                        if(it is ActivitySourcesFragment) {
                            it.refreshCurrentConnections()
                            return@forEach
                        }
                    }
                }
            }
        }
    }
}

package com.fjuul.sdk.android.exampleapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.fjuul.sdk.android.exampleapp.ui.activity_sources.ActivitySourcesFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data?.host?.contains("external_connect") == true &&
            intent?.data?.getQueryParameter("success") == "true") {
            val navigationController = nav_host_fragment.findNavController()
            if (navigationController.currentDestination?.id == R.id.activitySourcesFragment) {
                (nav_host_fragment.childFragmentManager.fragments[0] as ActivitySourcesFragment).refreshCurrentConnections()
            }
        }
    }
}

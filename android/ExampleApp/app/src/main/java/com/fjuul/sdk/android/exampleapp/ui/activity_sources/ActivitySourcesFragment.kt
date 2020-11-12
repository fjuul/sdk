package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.fjuul.sdk.activitysources.entities.FitbitActivitySource
import com.fjuul.sdk.activitysources.entities.GarminActivitySource
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource
import com.fjuul.sdk.activitysources.entities.PolarActivitySource
import com.fjuul.sdk.android.exampleapp.R

class ActivitySourcesFragment : Fragment() {
    private lateinit var currentSourceText: TextView
    private lateinit var sourcesList: ListView

    private val model: ActivitySourcesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activity_sources, container, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                GoogleFitActivitySource.getInstance().handleGoogleSignInResult(data) { result ->
                    Log.d(TAG,"GoogleFit connect = Error: ${result.error}; value: ${result.value}")
                    if (result.isError) {
                        // TODO: publish the error to the view model (or show the message in the alert)
                        return@handleGoogleSignInResult
                    }
                    if (!GoogleFitActivitySource.getInstance().isActivityRecognitionPermissionGranted) {
                        requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE)
                    }
                    model.fetchCurrentConnections()
                }
            }
            // TODO: else show the error message
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: ACTIVITY_RECOGNITION granted")
            } else {
                Log.d(TAG, "onRequestPermissionsResult: ACTIVITY_RECOGNITION not granted")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentSourceText = view.findViewById(R.id.current_activity_source_text)
        sourcesList = view.findViewById(R.id.activity_sources_list)

        model.fetchCurrentConnections()

        model.currentConnections.observe(
            viewLifecycleOwner,
            Observer { connections ->
                currentSourceText.text =
                    "Current source(s): ${connections.joinToString(", ") { it.tracker }}"
            }
        )

        model.errorMessage.observe(
            viewLifecycleOwner,
            Observer {
                if (it == null) {
                    return@Observer
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .show()
                model.resetErrorMessage()
            }
        )
        model.connectionIntent.observe(
            viewLifecycleOwner,
            Observer { pair ->
                if (pair == null) {
                    return@Observer
                }
                val (activitySource, intent) = pair
                when (activitySource) {
                    is GoogleFitActivitySource -> {
                        startActivityForResult(intent, GOOGLE_SIGN_IN_REQUEST_CODE)
                    } else -> {
                        startActivity(intent)
                    }
                }
            }
        )

        val adapter = ActivitySourcesListAdapter(
            requireContext(),
            arrayListOf(
                ActivitySourcesItem.FITBIT,
                ActivitySourcesItem.GARMIN,
                ActivitySourcesItem.POLAR,
                ActivitySourcesItem.GOOGLE_FIT,
                ActivitySourcesItem.DISCONNECT
            )
        )
        sourcesList.adapter = adapter
        sourcesList.setOnItemClickListener { parent, view, position, id ->
            val activitySource = when (adapter.getItem(position)) {
                ActivitySourcesItem.FITBIT -> FitbitActivitySource.getInstance()
                ActivitySourcesItem.POLAR -> PolarActivitySource.getInstance()
                ActivitySourcesItem.GARMIN -> GarminActivitySource.getInstance()
                ActivitySourcesItem.GOOGLE_FIT -> GoogleFitActivitySource.getInstance()
                else -> {
                    model.disconnect()
                    return@setOnItemClickListener
                }
            }
            if (model.isConnected(activitySource)) {
                model.disconnect(activitySource)
            } else {
                model.connect(activitySource)
            }
        }
    }

    fun refreshCurrentConnections() {
        model.fetchCurrentConnections()
    }

    companion object {
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 61076
        const val ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE = 33221
        const val TAG = "ActivitySourcesFragment"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivitySourcesFragment().apply {
                arguments = Bundle()
            }
    }
}

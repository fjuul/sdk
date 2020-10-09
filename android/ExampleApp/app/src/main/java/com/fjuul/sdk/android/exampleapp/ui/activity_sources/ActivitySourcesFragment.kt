package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ConnectionResult
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import java.time.Duration
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

class ActivitySourcesFragment : Fragment() {
    private lateinit var currentSourceText: TextView
    private lateinit var sourcesList: ListView
    private lateinit var activitySourcesManager: ActivitySourcesManager
    private lateinit var googleFitActivitySource: GoogleFitActivitySource

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
        Log.d(
            "ACTIVITY_SOURCES",
            "onActivityResult: ${requestCode}, ${resultCode}, intent: ${data}"
        )
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                activitySourcesManager.handleGoogleSignInResult(GoogleFitActivitySource(true), data) { error, success ->
                    println("Error: $error; success: $success")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentSourceText = view.findViewById(R.id.current_activity_source_text)
        sourcesList = view.findViewById(R.id.activity_sources_list)
        googleFitActivitySource = GoogleFitActivitySource(true)

        model.fetchCurrentConnections()

        model.currentConnections.observe(
            viewLifecycleOwner,
            Observer { connections ->
                currentSourceText.text =
                    "Current source(s): ${connections.joinToString(", ") { it.tracker }}"
            }
        )

        val granted = googleFitActivitySource.arePermissionsGranted(requireContext())
        println("GRANTED $granted")

        requestPermissions(listOf<String>(Manifest.permission.ACTIVITY_RECOGNITION).toTypedArray(), 2001)

        activitySourcesManager = ActivitySourcesManager(ActivitySourcesService(ApiClientHolder.sdkClient))
        val gfManager = activitySourcesManager.createGoogleFitDataManager(requireContext())
        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.add(Calendar.DAY_OF_YEAR, -1)
        startTimeCalendar.set(Calendar.MINUTE, 0)
        startTimeCalendar.set(Calendar.SECOND, 0)
        startTimeCalendar.set(Calendar.MILLISECOND, 0)

        val endTimeCalendar = Calendar.getInstance()
        endTimeCalendar.set(Calendar.MINUTE, 0)
        endTimeCalendar.set(Calendar.SECOND, 0)
        endTimeCalendar.set(Calendar.MILLISECOND, 0)
        // println("DATES ${startTimeCalendar.time} and ${endTimeCalendar.time}")
        // gfManager.getCalories(startTimeCalendar.time, endTimeCalendar.time)
        // gfManager.syncCalories(LocalDate.now().minusDays(5), LocalDate.now().minusDays(5))
        // gfManager.syncCalories(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4))
        // gfManager.syncCalories(LocalDate.now().minusDays(3), LocalDate.now().minusDays(3))
        // gfManager.syncCalories(LocalDate.now().minusDays(2), LocalDate.now().minusDays(2))
        // gfManager.syncCalories(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1))
        // gfManager.syncCalories(LocalDate.now().minusDays(30), LocalDate.now())


        // gfManager.syncSteps(LocalDate.now().minusDays(80), LocalDate.now());

        // gfManager.syncHR(LocalDate.now().minusDays(30), LocalDate.now());

        gfManager.syncSessions(LocalDate.now().minusDays(10), LocalDate.now(), Duration.ofMinutes(5));

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
        model.newConnectionResult.observe(
            viewLifecycleOwner,
            Observer { connectionResult ->
                if (connectionResult == null) {
                    return@Observer
                }
                when (connectionResult) {
                    is ConnectionResult.Connected -> {
                        model.fetchCurrentConnections()
                    }
                    is ConnectionResult.ExternalAuthenticationFlowRequired -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(connectionResult.url))
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
                ActivitySourcesItem.GOOGLE_FIT_BE,
                ActivitySourcesItem.GOOGLE_FIT_ADVANCED,
                ActivitySourcesItem.DISCONNECT
            )
        )
        sourcesList.adapter = adapter
        sourcesList.setOnItemClickListener { parent, view, position, id ->
            val tracker = when (adapter.getItem(position)) {
                ActivitySourcesItem.FITBIT -> "fitbit"
                ActivitySourcesItem.POLAR -> "polar"
                ActivitySourcesItem.GARMIN -> "garmin"
                ActivitySourcesItem.GOOGLE_FIT_BE -> "googlefit_backend"
                ActivitySourcesItem.GOOGLE_FIT_ADVANCED -> {
                    val signInIntent = activitySourcesManager.connect(googleFitActivitySource, requireActivity())
                    startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
                    return@setOnItemClickListener
                }
                else -> {
                    model.disconnect()
                    return@setOnItemClickListener
                }
            }
            model.connect(tracker)
        }
    }

    fun refreshCurrentConnections() {
        model.fetchCurrentConnections()
    }

    companion object {
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 61076

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivitySourcesFragment().apply {
                arguments = Bundle()
            }
    }
}

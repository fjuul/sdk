package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.fjuul.sdk.activitysources.entities.ConnectionResult
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentSourceText = view.findViewById(R.id.current_activity_source_text)
        sourcesList = view.findViewById(R.id.activity_sources_list)

        model.fetchCurrentConnections()

        model.currentConnections.observe(
            viewLifecycleOwner,
            Observer { connections ->
                currentSourceText.text = "Current source(s): ${connections.joinToString(", ") { it.tracker }}"
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
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivitySourcesFragment().apply {
                arguments = Bundle()
            }
    }
}

package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.fjuul.sdk.android.exampleapp.R

class ActivitySourcesFragment : Fragment() {
    private lateinit var currentSourceText: TextView
    private lateinit var sourcesList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activity_sources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentSourceText = view.findViewById(R.id.current_activity_source_text)
        sourcesList = view.findViewById(R.id.activity_sources_list)

        val adapter = ActivitySourcesListAdapter(
            requireContext(),
            arrayListOf(ActivitySourcesItem.FITBIT, ActivitySourcesItem.GARMIN, ActivitySourcesItem.POLAR)
        )
        sourcesList.adapter = adapter
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivitySourcesFragment().apply {
                arguments = Bundle()
            }
    }
}

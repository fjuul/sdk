package com.fjuul.sdk.android.exampleapp.ui.modules

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import com.fjuul.sdk.android.exampleapp.R

class ModulesFragment : Fragment() {
    private lateinit var modulesListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_modules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        modulesListView = view.findViewById<ListView>(R.id.modules_list)
        val listItems = listOf<String>("user", "analytics")
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, listItems)
        modulesListView.adapter = adapter

    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ModulesFragment().apply {
                arguments = Bundle()
            }
    }
}

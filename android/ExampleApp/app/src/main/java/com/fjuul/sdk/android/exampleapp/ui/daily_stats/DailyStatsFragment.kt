package com.fjuul.sdk.android.exampleapp.ui.daily_stats

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fjuul.sdk.android.exampleapp.R
import java.time.LocalDate

class DailyStatsFragment : Fragment() {
    private lateinit var fromInput: View
    private lateinit var toInput: View
    private lateinit var fromValueText: TextView
    private lateinit var toValueText: TextView
    private lateinit var dailyStatsList: ListView

    private val model: DailyStatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fromInput = view.findViewById<LinearLayout>(R.id.from_input_layout)
        toInput = view.findViewById<LinearLayout>(R.id.to_input_layout)
        fromValueText = view.findViewById(R.id.from_value_text)
        toValueText = view.findViewById(R.id.to_value_text)
        dailyStatsList = view.findViewById(R.id.daily_stats_list)

        model.requestData()
        model.startDate.observe(
            viewLifecycleOwner
        ) { date ->
            fromValueText.text = date.toString()
        }
        model.endDate.observe(
            viewLifecycleOwner
        ) {
            toValueText.text = it.toString()
        }

        val adapter = DailyStatsListAdapter(requireContext(), arrayOf())
        dailyStatsList.adapter = adapter
        model.data.observe(
            viewLifecycleOwner
        ) {
            adapter.dataSource = it
            adapter.notifyDataSetChanged()
        }

        fromInput.setOnClickListener {
            val date = model.startDate.value!!
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    model.setupDateRange(startDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        toInput.setOnClickListener {
            val date = model.endDate.value!!
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    model.setupDateRange(endDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DailyStatsFragment().apply {
                arguments = Bundle()
            }
    }
}

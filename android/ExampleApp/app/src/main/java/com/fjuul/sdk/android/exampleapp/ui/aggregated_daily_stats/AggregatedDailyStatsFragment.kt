package com.fjuul.sdk.android.exampleapp.ui.aggregated_daily_stats

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.analytics.entities.AggregationType
import java.time.LocalDate

class AggregatedDailyStatsFragment : Fragment() {
    private lateinit var fromInput: View
    private lateinit var toInput: View
    private lateinit var fromValueText: TextView
    private lateinit var toValueText: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var aggregatedStats: TextView

    private val model: AggregatedDailyStatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_aggregated_daily_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fromInput = view.findViewById<LinearLayout>(R.id.from_input_layout)
        toInput = view.findViewById<LinearLayout>(R.id.to_input_layout)
        fromValueText = view.findViewById(R.id.from_value_text)
        toValueText = view.findViewById(R.id.to_value_text)
        radioGroup = view.findViewById<RadioGroup>(R.id.aggregate_radio_group)
        aggregatedStats = view.findViewById(R.id.aggregated_stats)

        when (model.aggregation.value) {
            AggregationType.sum -> view.findViewById<RadioButton>(R.id.radio_sum).isChecked = true
            AggregationType.avg -> view.findViewById<RadioButton>(R.id.radio_avg).isChecked = true
        }

        model.requestData()
        model.startDate.observe(
            viewLifecycleOwner,
            Observer { date ->
                fromValueText.text = date.toString()
            }
        )
        model.endDate.observe(
            viewLifecycleOwner,
            Observer {
                toValueText.text = it.toString()
            }
        )

        model.data.observe(
            viewLifecycleOwner,
            Observer { it ->
                aggregatedStats.text =
                    """
 low: ${it.low.metMinutes} metMinutes;
 moderate: ${it.moderate.metMinutes} metMinutes;
 high: ${it.high.metMinutes} metMinutes"""
            }
        )

        model.errorMessage.observe(
            viewLifecycleOwner,
            Observer { it ->
                aggregatedStats.text = it.toString()
            }
        )

        fromInput.setOnClickListener {
            val date = model.startDate.value!!
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
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
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    model.setupDateRange(endDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_sum -> model.setAggregation(AggregationType.sum)
                R.id.radio_avg -> model.setAggregation(AggregationType.avg)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            AggregatedDailyStatsFragment().apply {
                arguments = Bundle()
            }
    }
}

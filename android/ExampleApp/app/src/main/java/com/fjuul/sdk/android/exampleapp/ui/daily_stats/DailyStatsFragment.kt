package com.fjuul.sdk.android.exampleapp.ui.daily_stats

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProviders
import com.fjuul.sdk.android.exampleapp.R
import java.time.LocalDate
import java.util.*

class DailyStatsFragment : Fragment() {
    private lateinit var fromInput: View
    private lateinit var toInput: View
    private lateinit var fromValueText: TextView
    private lateinit var toValueText: TextView

    private val model: DailyStatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_daily_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fromInput = view.findViewById<LinearLayout>(R.id.from_input_layout)
        toInput = view.findViewById<LinearLayout>(R.id.to_input_layout)
        fromValueText = view.findViewById(R.id.from_value_text)
        fromValueText = view.findViewById(R.id.to_value_text)

        model.startDate.observe(viewLifecycleOwner, androidx.lifecycle.Observer { date ->
            fromValueText.text = date.toString()
        })

        fromInput.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    model.setupDateRange(startDate = LocalDate.of(year, month, dayOfMonth));
                },
                year,
                month,
                day
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
